//
// ShellCommand
//
//
// Created by aleross on 3/1/13
// Copyright (c) 2012 Enplug, Inc. All rights reserved.
//

package com.aleross.shellcommand;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShellCommand {

    private static final String TAG = "ShellCommand";
    private ServerSocket socket;
    private BufferedReader dataInputStream;
    private PrintWriter dataOutputStream;
    private final ShellCallback callback;
    private int port;
    private static final String EXIT_CODE = "exit";
    private ScheduledExecutorService scheduledTaskExecutor;
    private Future timerTask;
    private static final int DEFAULT_PORT = 8000;

    public interface ShellCallback {
        List<String> onShellInput(final List<Comparable<?>> command);
    }

    private class TimerThreadFactory implements ThreadFactory {
        private int counter;
        private static final String prefix = "shell-command-timer";
        @Override
        public Thread newThread(final Runnable r) {
            final Thread newThread = new Thread(r, prefix + '-' + counter);
            counter++;
            return newThread;
        }
    }

    public ShellCommand(final ShellCallback callback) {
        this.callback = callback;
    }

    public void listen(final int port) {
        this.port = port;
        final Thread socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Opening socket on localhost port " + port);
                    final InetAddress socketAddress = Inet4Address.getLocalHost();
                    socket = new ServerSocket(port, 10, socketAddress);
                    final InetAddress establishedAddress = socket.getInetAddress();
                    Log.i(TAG, "Socket opened at " + establishedAddress.getHostAddress() + " on port " + port);
                    scheduleTimer();
                    while (true) {
                        //Server is waiting for client here, if needed
                        final Socket s = socket.accept();
                        Log.i(TAG, "Server received a connection and is connected " + s.isConnected());
                        dataInputStream = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        dataOutputStream = new PrintWriter(s.getOutputStream(),true); //Autoflush
                        Log.i(TAG, "Initialized input and output.");
                        boolean reading = true;
                        if (callback != null) {
                            dataOutputStream.println("Connected to application, ready to receive commands.");
                            while (reading) {
                                final String commandInput = dataInputStream.readLine();
                                if (commandInput == null) {
                                    return;
                                }
                                Log.i(TAG, "Received command from client: " + commandInput);
                                if ((EXIT_CODE).equals(commandInput)) {
                                    dataOutputStream.println("Exiting");
                                    reading = false;
                                } else {
                                    try {
                                        final List<Comparable<?>> args = getArguments(commandInput);
                                        final List<String> commandOutput = callback.onShellInput(args);
                                        dataOutputStream.println("Starting command response.");
                                        for (final String aCommandOutput : commandOutput) {
                                            dataOutputStream.println(aCommandOutput);
                                        }
                                        dataOutputStream.println("Finished sending command response.");
                                    } catch (Exception e) {
                                        Log.e(TAG, "There was an error while processing the command: " + commandInput, e);
                                        dataOutputStream.println("There was an error while processing your command: " + commandInput);
                                    }
                                }
                            }
                        } else {
                            dataOutputStream.println("This program is not configured correctly, it is missing a callback.");
                        }
                        s.close();
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "Something went wrong while opening the socket.", e);
                } catch (IOException e) {
                    Log.e(TAG, "Something went wrong while opening the socket.", e);
                }
                finally {
                    Log.i(TAG, "We would close the socket here.");
                    stop();
                }
            }
        });
        socketThread.start();
    }

    private List<Comparable<?>> getArguments(final String subjectString) {
        final List<String> matchList = new ArrayList<String>();
        final Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        final Matcher regexMatcher = regex.matcher(subjectString);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        Log.v(TAG, "Found " + matchList.size() + " arguments.");
        return transformArguments(matchList);
    }

    private List<Comparable<?>> transformArguments(final List<String> originalArgs) {
        final List<Comparable<?>> trueArgs = new ArrayList<Comparable<?>>();
        for (int i = 0; i < originalArgs.size(); i++) {
            final String arg = originalArgs.get(i);
            if (isInt(arg)) {
                trueArgs.add(i, Integer.parseInt(arg));
            } else if (isDouble(arg)) {
                trueArgs.add(i, Double.parseDouble(arg));
            } else {
                // Add back in as a string
                trueArgs.add(i, arg);
            }
        }
        Log.i(TAG, "Transformed " + trueArgs.size() + " args");
        return trueArgs;
    }

    private boolean isInt(final String string) {
        if (string == null) {
            return false;
        }
        int length = string.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (string.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = string.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }

    private boolean isDouble(final String string) {
        try {
            Double.parseDouble(string);
            return true;
        }
        catch(NumberFormatException e) {
            return false;
        }
    }

    private void scheduleTimer() {
        Log.i(TAG, "Scheduling timer to make sure ShellCommand socket stays open.");
        scheduledTaskExecutor = Executors.newSingleThreadScheduledExecutor(new TimerThreadFactory());
        timerTask = scheduledTaskExecutor.scheduleAtFixedRate(statusCheck, 0L, 1L, TimeUnit.MINUTES);
    }

    private final Runnable statusCheck = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Checking for Command Reader socket.");
            try {
                if ((socket == null) || socket.isClosed()) {
                    Log.e(TAG, "Command reader socket is not open, socket is " + socket + ". Reopening on port " + port);
                    listen(port);
                } else {
                    Log.d(TAG, "Command Reader socket is open.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Encountered an error while checking whether the Command reader socket is open.", e);
            }
        }
    };

    private static final String EXTRA_PORT = "PORT";

    public BroadcastReceiver startReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.i(TAG, "Received request to start Shell Command socket.");
            int newPort = DEFAULT_PORT;
            if (intent.hasExtra(EXTRA_PORT)) {
                final String portString = intent.getStringExtra(EXTRA_PORT);
                try {
                    newPort = Integer.parseInt(portString);
                    Log.i(TAG, "Request specified port " + newPort);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Could not parse port into a string", e);
                    newPort = DEFAULT_PORT;
                }
                Log.i(TAG, "Request specified port " + newPort);
            }
            listen(newPort);
        }
    };

    public void stop() {
        if (socket != null){
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Something went wrong while closing the server.", e);
            }
        }
        if (dataInputStream != null) {
            try {
                dataInputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Something went wrong while closing the input stream.", e);
            }
        }
        if (timerTask != null) {
            timerTask.cancel(true);
        }
        if (scheduledTaskExecutor != null) {
            scheduledTaskExecutor.shutdownNow();
            scheduledTaskExecutor = null;
        }
    }
}
