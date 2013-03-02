A wonderfully easy to use tool for adding command-line interactivity to your app.

Use netcat (available on the Android emulator) or telnet to connect to the server socket. For example, to connect on your emulator you would call 

    nc 127.0.0.1 8000 

Most physical android devices use `10.0.2.2` as localhost.

Arguments are broken up into an array after taking into account single and double quotes. Integers and doubles are appropriately cast into their respective types. It's up to you to validate/check this array for arguments your app cares about.

Use the command "exit" to leave the interactive shell. The socket will remain up and available for the next connection.

Limited formatting is supported in ShellCommandFormatters, sample calls are shown in MyActivity.

An executor service runs every 10 minutes checking whether the socket is still up, it will restart it if it is not.

The sample activity should be ready to go, just load it into your emulator and connect via ./adb shell. If you have more than device use the -s flag: 

    ./adb -s [deviceName] shell. 
    
You can find your emulator's device name by using 

    ./adb devices.

Note that Activity's implementing ShellCommand must include the uses INTERNET statement in their AndroidManifest.xml.

    <uses-permission android:name="android.permission.INTERNET"/>
