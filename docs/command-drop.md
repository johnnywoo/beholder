# `drop` — destroys messages

    drop

Drops the message from processing. Useful inside `switch`:

    switch $format {
        case ~json~ {
            parse json;
            # Message gets out of `switch` into messages.log
        }
        case ~syslog~ {
            parse syslog;
            # Message gets out of `switch` into messages.log
        }
        case ~special-case~ {
            to udp 1234;
            drop; # Message does not go to messages.log
        }
    }
    to file messages.log;
