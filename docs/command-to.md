# `to` — sends messages to destinations

    to stdout;
    to file <file>;
    to udp [<address>:]<port>;
    to tcp [<address>:]<port>;
    to shell <command>;

This command writes `$payload` field of incoming messages to some destination.
To format the payload, use `set $payload ...` command.

    flow {
        from timer;
        set $payload '$date Just a repeating text message';
        to stdout;
    }

This example config will produce messages like these:

    2017-11-27T21:14:01+03:00 Just a repeating text message
    2017-11-27T21:14:02+03:00 Just a repeating text message
    2017-11-27T21:14:03+03:00 Just a repeating text message

`to stdout` simply sends payloads of messages into stdout of beholder process.
A newline is appended to every payload unless it already ends with a newline.

`to file <file>` stores payloads of messages into a file.
A newline is appended to every payload unless it already ends with a newline.
Relative filenames are resolved from CWD of beholder process.

You can use message fields in filenames:

    flow {
        from udp 1234;
        parse syslog;
        set $payload syslog;
        to file '/var/log/export/$host/$program.log';
    }

`to udp [<address>:]<port>` sends payloads of messages as UDP packets.
Default address is 127.0.0.1.

`to tcp [<address>:]<port>` sends payloads of messages over a TCP connection.
Default address is 127.0.0.1.
A newline is appended to every payload unless it already ends with a newline.

`to shell <command>` sends payloads of messages into a process started with a shell command.
The command should not exit immediately, but instead keep reading messages from stdin.
A newline is appended to every payload unless it already ends with a newline.

Beware of stdin buffering! If your shell command is a bash script, bash will buffer
incoming messages before passing them into the script. Test your scripts early!

    #!/usr/bin/php
    <?php
    // Example log receiver script in PHP
    while ($f = fgets(STDIN)) {
        file_put_contents('receiver.log', date('r') . ' ' . $f, FILE_APPEND);
    }
