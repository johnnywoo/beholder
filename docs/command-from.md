# `from` — produces messages from various sources

    from udp [<address>:]<port>;
    from tcp [<address>:]<port> [as syslog-frame];
    from timer [<n> seconds];
    from infinity [<message-length-bytes>];
    from internal-log;

This command produces messages.

If there are any incoming messages (not produced by current `from` command),
`from` will copy them to its output.

To receive messages in different formats from different sources, use `join`.

    flow {
        from udp 1001;
        join {
            from udp 1002;
            parse syslog;
        }
        set $payload dump;
        to stdout;
        # in stdout we will see raw messages from port 1001
        # and processed syslog messages from port 1002
    }

Fields produced by `from udp`:

* `$date`    — ISO date when the packet was received (example: 2017-11-26T16:22:31+03:00)
* `$from`    — URI of packet source (example: udp://1.2.3.4:57733)
* `$payload` — Text as received from UDP

`from tcp` reads messages from a TCP server socket. Messages should be terminated by newlines.
To transfer multiline messages over TCP, encode them into a single-line format such as JSON.

Fields produced by `from tcp`:

* `$date`    — ISO date when the packet was received (example: 2017-11-26T16:22:31+03:00)
* `$from`    — URI of packet source (example: tcp://1.2.3.4:57733)
* `$payload` — Text as received from the TCP connection

Default reading mode for TCP is newline-terminated messages (\r\n or \n).
`from tcp ... as syslog-frame` instead reads messages in syslog frame format,
which is length-space-data (see RFC5425 "4.3. Sending Data"). Syslog frame will ignore
any characters not conforming to the length-space-data protocol.
Example: `5 hello5 world` encodes two messages with payloads of 'hello' and 'world'.

`from timer` emits a minimal message every second.
It is useful for experimenting with beholder configurations.

You can specify a number of seconds between messages like this:

* `from timer 30 seconds` for two messages per minute;
* `from timer 1 second` is the default and equivalent to just `from timer`.

Fields produced by `from timer`:

* `$date`    — ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
* `$from`    — 'beholder://timer'
* `$program` — 'beholder'
* `$payload` — A short random message

`from infinity` is a debug source that simply emits messages in an infinite loop.

`from internal-log` emits messages from the internal Beholder log. These are the same messages
Beholder writes to stdout/stderr and its log file (see also CLI options `--log` and `--quiet`).

Fields produced by `from internal-log`:

* `$date`     — ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
* `$from`     — 'beholder://internal-log'
* `$severity` — Severity of messages (number)
* `$program`  — 'beholder'
* `$payload`  — Log message text
