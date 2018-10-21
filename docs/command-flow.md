# `flow` — creates isolated flows of messages

    flow {<subcommands>}

You can think of `flow` as namespaces or visibility scopes.

Subcommands: all commands are allowed.

`flow` should be used to create separate processing chains:

    # WRONG
    from udp 1001;
    to tcp 1.2.3.4:1002;
    from udp 1003;
    to tcp 1.2.3.4:1004; # Receives messages from both ports 1001 AND 1003!

    # separate flows
    flow {from udp 1001; to tcp 1.2.3.4:1002}
    flow {from udp 1003; to tcp 1.2.3.4:1004}

Message routing for `flow`:

    from udp 1001;
    flow {
        # Incoming messages are only emitted out of `flow`,
        # subcommands do not receive them.
        from udp 1002;
        to file some.log; # receives messages only from port 1002
        # After last subcommand messages are dropped
    }
    to stdout; # Receives messages only from port 1001
