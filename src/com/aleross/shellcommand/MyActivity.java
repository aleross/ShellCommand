package com.aleross.shellcommand;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MyActivity extends Activity {

    private static final int SHELL_PORT = 8000;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ShellCommand shellCommand = new ShellCommand(shellCallback);
        shellCommand.listen(SHELL_PORT); // Open the socket, listen for commands
    }

    private ShellCommand.ShellCallback shellCallback = new ShellCommand.ShellCallback() {
        @Override
        public List<String> onShellInput(final List<Comparable<?>> args) {
            final List<String> response = new ArrayList<String>(5);
            if (("report").equals(args.get(0))) { // All checking for command validity has to be done in this callback
                response.add("Generating Player Report");
                response.add(ShellResponseFormatter.formatSectionHeader("Sample Header Level 1")); // Adds --- around header
                response.add("*Example Header Level 2"); // One * for a header level 2
                response.add("**Example Response Item"); // Two * for an indented item
                response.add("Example unformatted response.");
            } else {
                response.add("Invalid command. Try using 'report'.");
            }
            if (args.get(0) == Integer.valueOf(1)) {
                response.add("Wow!!");
            }
            return ShellResponseFormatter.formatResponse(response);
        }
    };

}
