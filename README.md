# Beholder

Beholder is a log processor. Its purpose is to receive, process, transfer and export log messages.

WARNING: Beholder has no stable version yet.
Config syntax, commands, options, behaviour, everything is going to be changed without any backwards compatibility.

 * [Building Beholder](#building-beholder)
 * [Usage](#usage)
 * [Recipes](#recipes)
 * [Config structure](#config-structure)
 * [Config syntax](#config-syntax)
 * [Config commands](#config-commands)
   + [`flow`](#flow)
   + [`from`](#from)
   + [`to`](#to)
   + [`set`](#set)
   + [`keep`](#keep)
   + [`drop`](#drop)
   + [`switch`](#switch)
   + [`parse`](#parse)
   + [Settings](#settings)

## Building Beholder

A ready-made Docker image is available at https://hub.docker.com/r/johnnywoo/beholder/

To use the docker container, mount your config into it as `/etc/beholder/beholder.conf`.

To build the jar file with all dependencies:

    $ ./gradlew jar
    $ ls build/libs/beholder*.jar

To build a docker container with the jar:

    $ docker build -t beholder .
    $ docker run -ti beholder beholder
    usage: beholder
    ...


## Usage

    usage: beholder
     -c,--config <text>        Use config from the argument
     -f,--config-file <file>   Use config from a file
     -h,--help                 Show usage
     -l,--log <file>           Internal log file
     -q,--quiet                Do not print internal log into stdout
     -t,--test                 Config test: syntax and minimal validation
     -v,--version              Show version

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
        flow out {  # this command has both args and subcommands
            from udp 3820;  # this command has only arguments
            parse syslog;
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
Some arguments to certain commands do not allow field names; be sure to validate your configs.

    'date: $date payload: $payload'

Simple syntax: `'$field'`. To prevent unwanted greediness: `'{$cat}astrophe'`.
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

Note also that literal `{$field}` is invalid (it will be parsed as subcommand block),
while `$field` and quoted `'{$field}` are valid.

Example: `127.0.0.1:1234`.


### Config commands

* `flow`   — defines flow of messages between commands
* `from`   — produces messages from some source
* `to`     — sends messages to destinations
* `set`    — puts values into message fields
* `keep`   — removes unnecessary message fields
* `drop`   — removes the message altogether
* `switch` — conditional processing
* `parse`  — populates message fields according to some format
* settings — global configuration options


### `flow`

    flow {<subcommands>}        -- copy incoming, discard results
    flow out {<subcommands>}    -- produce results, ignore incoming
    flow closed {<subcommands>} -- ignore everything

Use this command to create separate flows of messages.

Subcommands: `flow`, `from`, `to`, `set`, `keep`, `drop`, `switch`, `parse`.

Some use cases of `flow`:

* Creating separate processing chains:

      # WRONG
      from udp 1001;
      to tcp 1.2.3.4:1002;
      from udp 1003;
      to tcp 1.2.3.4:1004; # receives messages from both ports 1001 AND 1003!

      # separate flows
      flow {from udp 1001; to tcp 1.2.3.4:1002}
      flow {from udp 1003; to tcp 1.2.3.4:1004}

* Inserting a temporary dump in the middle of your config:

      from udp 1001;
      parse syslog;

      # Here we want to look at the message between `parse` and `set`.
      # If we just inserted dump commands here, they would modify the message,
      # which we do not want. Instead, we can copy messages into a flow
      # and modify the copies, which are then discarded.
      flow {
          set $payload dump;
          to file dump.log;
      }

      set $payload json;
      to tcp 1234;

* Applying some commands to message from a specific source:

      flow out { from udp 1001; parse syslog; }
      flow out { from udp 1002; parse json; }
      to file '$host.log';

Message routing for `flow` in default mode:

    from udp 1001;
    flow {
        # incoming messages are duplicated:
        # one is emitted out, one is passed into first subcommand
        from udp 1002;
        to file some.log; # receives messages from ports 1001 AND 1002
        # after last subcommand messages are discarded
    }
    to stdout; # this command receives messages only from port 1001

Message routing for `flow out`:

    from udp 1001;
    flow out {
        # incoming messages are only emitted out of the flow,
        # subcommands do not receive them
        from udp 1002;
        to file some.log; # receives messages only from port 1002
        # after last subcommand messages are emitted out of the flow
    }
    to stdout; # receives messages from ports 1001 AND 1002

Message routing for `flow closed`:

    from udp 1001;
    flow closed {
        # incoming messages are only emitted out of the flow,
        # subcommands do not receive them
        from udp 1002;
        to file some.log; # receives messages only from port 1002
        # after last subcommand messages are discarded
    }
    to stdout; # receives messages only from port 1001

Incoming messages are always emitted out of the `flow`.
Inside the `flow` messages are consecutively passed between subcommands.


### `from`

    from udp [<address>:]<port>;
    from tcp [<address>:]<port>;
    from timer [<n> seconds];
    from internal-log;

This command produces messages.

If there are any incoming messages (not produced by current `from` command),
`from` will copy them to its output.

To receive messages in different formats from different sources, use `flow out`.

    flow {
        from udp 1001;
        flow out {
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

`from internal-log` emits messages from the internal Beholder log. These are the same messages
Beholder writes to stdout/stderr and its log file (see also CLI options `--log` and `--quiet`).

Fields produced by `from internal-log`:

* `$date`     — ISO date when the message was emitted (example: 2017-11-26T16:22:31+03:00)
* `$from`     — 'beholder://internal-log'
* `$severity` — Severity of messages
* `$program`  — 'beholder'
* `$payload`  — Log message text


### `to`

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
    // example log receiver script in PHP
    while ($f = fgets(STDIN)) {
        file_put_contents('receiver.log', date('r') . ' ' . $f, FILE_APPEND);
    }


### `set`

    set $field 'template with $fields from the message';
    set $field <function> [... function args];

This command manipulates individual message fields.

When given a quoted string, `set` will substitute field names in the string
with corresponding values from the message.

    flow {
        flow out {from timer; set $color 'red'}
        flow out {from timer; set $color 'green'}
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

* `syslog` — Generates a IETF syslog payload based on syslog-related fields; see `parse syslog` for details.
* `replace` — String replacement with regexp. See below.
* `time` — Current time, e.g. 01:23:45.
* `host` — Current hostname.
* `env` — Environment variable value: `set $path env PATH`.
* `basename` — Last component of a filename: `set $file basename /path/file.ext`.
    The file does not need to exist. Bad names like `..` are replaced with `noname`.
* `severity-name` — String name of numeric syslog severity. `set $name severity-name $severity [lowercase]`.
* `dump` — Generates a dump payload with all fields of the message.
* `json` — Generates a JSON string with message fields. See below.
* `prefix-with-length` — Prefixes payload with its length in bytes (for syslog over TCP, see RFC5425 "4.3. Sending Data").

`set $field replace <regexp> <replacement> [in <subject>];`
Takes subject string, replaces all occurences of regexp in it with the replacement,
and stores the new value into $field. Default subject is $field itself.

    set $payload replace ~warn(ing)?~i 'WARNING';
    set $host replace ~^www\.~ '' in '$subdomain.$domain';

Be aware of double-escaping in replacement strings! Example:

    set $payload replace ~\n~ '\\\\n';

This command converts newlines into `\n` sequences.

`set $payload json` generates a JSON string with all fields of the message.
Alternatively you may specify which fields to include: `set $payload json $field $field2`.
Resulting JSON string will not contain any literal newline characters.

    set $animal cat;
    set $description 'A cat.\nYou should know what a cat is.';
    set $payload json $animal $description;
    # Multiline fields will be encoded with escape sequences:
    # {"animal":"cat","description":"A cat.\nYou should know what a cat is."}


### `keep`

    keep $field [$field2 ...]

Only keeps certain fields in the message. All fields that are not specified in arguments are removed.


### `drop`

    drop

Drops the message from processing. Useful inside `switch`:

    switch $format {
        case ~json~ {
            parse json;
            # message gets out of `switch` into messages.log
        }
        case ~syslog~ {
            parse syslog;
            # message gets out of `switch` into messages.log
        }
        case ~special-case~ {
            to udp 1234;
            drop; # message does not go to messages.log
        }
    }
    to file messages.log;


### `switch`

    switch 'template with $fields' {
        case ~regexp~ {
            <subcommands>
        }
        default {
            <subcommands>
        }
    }

This command allows conditional processing of messages.

Subcommands of `switch`: `case`, `default`.

Subcommands of `case`/`default`: `flow`, `from`, `to`, `set`, `keep`, `drop`, `switch`, `parse`.

`case` regexps are matched against the template provided as the argument to `switch`.
First matching `case` wins: its subcommands receive the message.
If there was no match, an optional `default` block receives the message.
There can be multiple `case` blocks, but only one `default`, and it must be the last block in `switch`.

If a message does not match any `case` and there is no `default`, the message will be discarded.
This way `switch` can work as an if-statement:

    switch $host { case ~.~ {} }
    to stdout; # Only prints messages with non-empty $host

Although `from` subcommand is permitted inside `case`/`default`, its use there is discouraged.
Messages emitted inside `case`/`default` ignore conditions and are emitted out of `switch`.

If your regexp has named groups, those groups will be placed as fields into the message:

    switch $program {
        case ~^nginx-(?<kind>access|error)$~ {
            # $kind now is either 'access' or 'error'
        }
    }


### `parse`

    parse [keep-unparsed] syslog;
    parse [keep-unparsed] json;
    parse [keep-unparsed] ~regexp-with-named-groups~;
    parse beholder-stats;

This command sets fields on messages according to chosen format.
If a message cannot be parsed, it will be dropped by default.
If `keep-unparsed` option is specified, unparsed messages will be kept unchanged.

Format `syslog`: the only syslog variant currently supported is
a BSD-style syslog format as produced by nginx.

Incoming messages look like this:

    <190>Nov 25 13:46:44 host nginx: <actual log message>

Fields produced by `parse syslog`:

* `$facility` — numeric syslog facility
* `$severity` — numeric syslog severity
* `$host`     — source host from the message
* `$program`  — program name (nginx calls this "tag")
* `$payload`  — actual log message (this would've been written to a file by nginx)

Format `json`: parses $payload as a JSON object and sets its properties as message fields.
The JSON object may only contain numbers, strings, booleans and nulls (no nested objects or arrays).
Boolean values are converted to strings 'true' and 'false'.

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


### Settings

Global settings may be provided at the top level of the config.
It is done in form of a command.

`from` buffers are used when Beholder cannot process incoming messages quickly enough.
These buffers hold incoming messages while Beholder is reloading.
Every source has its own buffer; if you are listening on two different TCP ports,
Beholder will create a separate buffer for each of those ports.

    from_internal_log_buffer_messages_count 1000;
    from_tcp_buffer_messages_count 1000;
    from_udp_buffer_messages_count 1000;

`to` buffers hold messages to be written to a destination. If a destination cannot accept
new data quickly enough, Beholder will put it into a buffer. When the buffer is full,
old messages are deleted to make space for new ones.
Every destination has its own buffer; if you are writing messages into two different files,
Beholder will create a separate buffer for each of those files.

    to_file_buffer_messages_count 1000;
    to_shell_buffer_messages_count 1000;
    to_tcp_buffer_messages_count 1000;
    to_udp_buffer_messages_count 1000;


