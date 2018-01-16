# Beholder

Beholder is a log processor. Its purpose is to receive, process, transfer and export log messages.

## Usage

    usage: beholder
     -c,--config <text>        Use config from the argument
     -f,--config-file <file>   Use config from a file
     -h                        Show usage
     -l,--log <file>           Internal log file
     -q,--quiet                Do not print internal log into stdout
     -t,--test                 Config test: syntax and minimal validation

## Recipes

Listen on a UDP port and write every incoming packet into a file, separated by newlines:

    flow {
        from udp 1234;
        to file '.../from-udp-1234.log';
    }

Receive nginx access log over UDP and write it into a file:

    flow {
        from udp 3820;
        parse syslog;
        to file '.../access.log';
    }

Send internal beholder log as syslog over UDP:

    flow {
        from internal-log;
        set $payload syslog;
        to udp 1234;
    }


## Configuration

### Config structure

Config contains commands, which can have subcommands (and so on).
Commands can produce, consume and modify messages.
Messages are collections of arbitrary fields.

    flow {  # this command has no args, only subcommands
        from udp 3820 {  # this command has both args and subcommands
            parse syslog;  # this command has only arguments
        }
        to stdout;
    }


### Config syntax

Command arguments can be expressed as literal words, quoted strings and regexps.

Quoted strings start with either `'` or `"`. There is no difference between the two.
Escaping is done with backslashes. Special characters: `\n`, `\r`, `\t`.
Backslashes prefixing any other characters are stripped off.

* `'this \' is a quote'` => this ' is a quote
* `'this \" is also a quote'` => this " is also a quote
* `'\z'` => z
* `'\n'` => newline character
* `'\\n'` => \n

Quoted strings may contain message field names, which are replaced with their values.

    'date: $receivedDate payload: $payload'

Field names consist of alphanumeric characters (case-sensitive) and underscores.
Field names cannot start with numbers.

Regexps are recognized by a delimiter, which currently can only be `~`.
The delimiter currently cannot be escaped in the regexp.
Regexp are written in the form of `~body~modifiers`. Modifiers are optional.

    ~spaces are allowed~
    ~http://[a-z.]+~
    ~cat|dog~i

Literal word is a string of non-whitespace characters that is not a quoted string or regexp.
Literal words may contain field names, which are replaced with their values.
There is no escaping in literal words.

Example: `127.0.0.1:1234`.


### Config commands

* `flow`   — defines flow of messages between commands
* `from`   — produces messages from some source
* `set`    — puts values into message fields
* `keep`   — removes unnecessary message fields
* `parse`  — populates message fields according to some format
* `to`     — sends messages to destinations


### `flow {subcommands}`

Subcommands: `flow`, `from`, `parse`, `set`, `keep`, `to`.

Use this command to create separate flows of messages.
Rule of thumb is: what happens in `flow` stays in `flow`.

Message routing:
Incoming message: a copy goes into first subcommand, a copy is emitted out of the `flow`.
Exit of each subcommand is routed into next subcommand.
Exit of last subcommand: messages are discarded.

Example config with a caveat:

    from udp 1001;
    to tcp 1.2.3.4:1002; # here we send messages from port 1001
    from udp 1003;
    to tcp 1.2.3.4:1004; # receives messages from both ports 1001 AND 1003!

This config will create two separate flows of messages:

    flow {from udp 1001; to tcp 1.2.3.4:1002}
    flow {from udp 1003; to tcp 1.2.3.4:1004}


### `from`

    from udp [<address>:]<port>;
    from timer [<n> seconds];
    from internal-log;

Subcommands: `parse`, `set`, `keep`.

This command produces messages, applying subcommands to them if there are any.

If there are any incoming messages (not produced by current `from` command),
`from` will copy them to its output. Subcommands are not applied to those.

You can use subcommands to pre-process messages before placing them into the flow.
This way, you can easily receive messages in different formats from different sources.

    flow {
        from udp 1001;
        from udp 1002 {parse syslog}
        set $payload dump;
        to stdout;
        # in stdout we will see raw messages from port 1001
        # and processed syslog messages from port 1002
    }

Fields produced by `from udp`:

* `$receivedDate`  — ISO date when the packet was received (example: 2017-11-26T16:22:31+03:00)
* `$from`          — URI of packet source (example: udp://1.2.3.4:57733)
* `$payload`       — Text as received from UDP

`from timer` emits a minimal message every second.
It is useful for experimenting with beholder configurations.

You can specify a number of seconds between messages like this:

* `from timer 30 seconds` for two messages per minute;
* `from timer 1 second` is the default and equivalent to just `from timer`.

Fields produced by `from timer`:

* `$receivedDate`  — ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
* `$from`          — 'beholder://timer'
* `$syslogProgram` — 'beholder'
* `$payload`       — A short random message

`from internal-log` emits messages from the internal Beholder log. These are the same messages
Beholder writes to stdout/stderr and its log file (see also CLI options `--log` and `--quiet`).

Fields produced by `from internal-log`:

* `$receivedDate`   — ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
* `$from`           — 'beholder://internal-log'
* `$syslogSeverity` — Severity of messages
* `$syslogProgram`  — 'beholder'
* `$payload`        — Log message text


### `parse`

    parse syslog;
    parse ~regexp-with-named-groups~;
    parse beholder-stats;

This command sets fields on messages according to chosen format.
If a message cannot be parsed, it will be left unchanged.

Format `syslog`: the only syslog variant currently supported is
a BSD-style syslog format as produced by nginx.

Incoming messages look like this:

    <190>Nov 25 13:46:44 host nginx: <actual log message>

Fields produced by `parse syslog`:

* `$syslogFacility`  — numeric syslog facility
* `$syslogSeverity`  — numeric syslog severity
* `$syslogHost`      — source host from the message
* `$syslogProgram`   — program name (nginx calls this "tag")
* `$payload`         — actual log message (this would've been written to a file by nginx)

Format `~regexp-with-named-groups~`: if the regexp matches, named groups from it
become message fields. Group names should not be prefixed with $.

    parse ~(?<logKind>access|error)~;

This will produce field $logKind with either 'access' or 'error' as value,
if either word occurs in $payload. If both words are present, earliest match is used.

Format `beholder-stats`: fills the message with internal Beholder stats.
Use this with `from timer` to create a health log.

Fields produced by `parse beholder-stats`:

* `$uptimeSeconds`  — Uptime in seconds
* `$heapBytes`      — Current heap size in bytes (memory usage)
* `$heapUsedBytes`  — Used memory in the heap
* `$heapMaxBytes`   — Maximal heap size
* `$udpMaxBytesIn`  — Maximal size of received UDP packet since last collection of stats
* `$payload`        — A summary of Beholder stats


### `set`

    set $field 'template with $fields from the message';
    set $field <function> [... function args];

This command manipulates individual message fields.

When given a quoted string, `set` will substitute field names in the string
with corresponding values from the message.

    flow {
        from timer {set $color 'red'}
        from timer {set $color 'green'}
        set $payload 'We got $color apples!';
        to stdout;
    }

This example will produce messages like these:

    We got red apples!
    We got green apples!
    We got red apples!
    We got green apples!

To unset a field, set it to an empty string: `set $host ''`.

When given a built-in function, `set` can construct different values for message fields.

Functions:

* `syslog`  — Generates a IETF syslog payload based on syslog-related fields;
               see `parse syslog` for details.
* `replace` — String replacement with regexp. See below.
* `time`    — Current time, e.g. 01:23:45.
* `dump`    — Generates a dump payload with all fields of the message.
* `json`    — Generates a JSON string with all fields of the message.
               Alternatively you may specify which fields to keep:
               `set $payload json $field $field2`
* `prefix-with-length`  — Prefixes payload with its length in bytes
               (for syslog over TCP, see RFC5425 "4.3. Sending Data").

`set $field replace <regexp> <replacement> [in <subject>];`
Takes subject string, replaces all occurences of regexp in it with the replacement,
and stores the new value into $field. Default subject is $field itself.

    set $payload replace ~warn(ing)?~i 'WARNING';
    set $host replace ~^www\.~ '' in '$subdomain.$domain';

Be aware of double-escaping in replacement strings! Example:

    set $payload replace ~\n~ '\\\\n';

This command converts newlines into `\n` sequences.


### `keep`

    keep $field [$field2 ...]

Removes message fields that are not specified in arguments.


### `to`

    to stdout;
    to file <file>;
    to udp [<address>:]<port>;
    to tcp [<address>:]<port>;

This command writes `$payload` field of incoming messages to some destination.
To format the payload, use `set $payload ...` command.

    flow {
        from timer {set $payload '$receivedDate Just a repeating text message'}
        to stdout;
    }

This example config will produce messages like these:

    2017-11-27T21:14:01+03:00 Just a repeating text message
    2017-11-27T21:14:02+03:00 Just a repeating text message
    2017-11-27T21:14:03+03:00 Just a repeating text message

`to stdout` simply sends payloads of messages into STDOUT of beholder process.
A newline is appended to every payload unless it already ends with a newline.

`to file <file>` stores payloads of messages into a file.
A newline is appended to every payload unless it already ends with a newline.
Relative filenames are resolved from CWD of beholder process.

You can use message fields in filenames:

    flow {
        from udp 1234;
        parse syslog;
        set $payload syslog;
        to file '/var/log/export/$syslogHost/$syslogProgram.log';
    }

`to udp [<address>:]<port>` sends payloads of messages as UDP packets.
Default address is 127.0.0.1.

`to tcp [<address>:]<port>` sends payloads of messages over a TCP connection.
Default address is 127.0.0.1.
A newline is appended to every payload unless it already ends with a newline.


## Building

To build a jar file with all dependencies:

    ./gradlew jar
    ls build/libs/beholder*.jar

To build a docker container with the jar:

    ./gradlew jar
    docker build -t beholder .
