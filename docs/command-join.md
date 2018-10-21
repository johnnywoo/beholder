# `join` — produces messages from subcommands

    join {<subcommands>}

Subcommands: all commands are allowed.

`join` can be used to apply some commands to messages from a specific source:

    join { from udp 1001; parse syslog; }
    join { from udp 1002; parse json; }
    to file '$host.log';

Message routing for `join`:

    from udp 1001;
    join {
        # Incoming messages are only emitted out of `join`,
        # subcommands do not receive them.
        from udp 1002;
        to file some.log; # Receives messages only from port 1002
        # After last subcommand messages are emitted out of `join`
    }
    to stdout; # Receives messages from ports 1001 AND 1002
