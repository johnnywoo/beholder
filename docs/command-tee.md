# `tee` — applies commands to copies of messages

    tee {<subcommands>}

This command allows you to run some commands on messages without disturbing the original flow.

Subcommands: all commands are allowed.

`tee` works by copying incoming messages. The original message is emitted out of `tee`
as if nothing ever happened; the copy is passed through subcommands and then dropped.

`tee` is useful for inserting a temporary dump in the middle of your config:

    from udp 1001;
    parse syslog;

    # Here we want to look at the message between `parse` and `set`.
    # If we just inserted dump commands here, they would modify the message,
    # which we do not want. Instead, we can use `tee`.
    tee {
        set $payload dump; # Only applied to the copy, not to original message
        to file dump.log;
    }

    set $payload json;
    to tcp 1234;

Message routing for `tee`:

    from udp 1001;
    tee {
        # Incoming messages are duplicated:
        # one is emitted out, one is passed into first subcommand.
        from udp 1002;
        to file some.log; # Receives messages from ports 1001 AND 1002
        # After last subcommand messages are dropped
    }
    to stdout; # This command receives messages only from port 1001
