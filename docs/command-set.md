# `set` — puts values into message fields

    set $field 'template with $fields from the message';
    set $field <function> [... function args];

This command manipulates individual message fields.

When given a quoted string, `set` will substitute field names in the string
with corresponding values from the message.

    flow {
        join {from timer; set $color 'red'}
        join {from timer; set $color 'green'}
        set $payload 'We got $color apples!';
        to stdout;
    }

This example will produce messages like these:

    We got red apples!
    We got green apples!
    We got red apples!
    We got green apples!

To unset a field, set it to an empty string: `set $host ''`.


## Functions

When given a built-in function, `set` can construct different values for message fields.

Functions:

* `syslog` — Generates a IETF syslog payload based on syslog-related fields; see `parse syslog` for details.
* `replace` — String replacement with regexp. See below.
* `time` — Current time, e.g. 01:23:45. There are more options, see below.
* `date` — Current date, e.g. 2018-01-30. There are more options, see below.
* `host` — Current hostname. Warning: if you're running Beholder in a Docker container,
    you should provide correct hostname into it, e.g. `net=host` or `docker run --hostname`.
* `env` — Environment variable value: `set $path env PATH`.
* `basename` — Last component of a filename: `set $file basename /path/file.ext`.
    The file does not need to exist. Bad names like `..` are replaced with `noname`.
* `severity-name` — String name of numeric syslog severity. `set $name severity-name $severity [lowercase]`.
* `dump` — Generates a dump payload with all fields of the message.
* `json` — Generates a JSON string with message fields. See below.
* `fieldpack` — Generates a Fieldpack binary packet with the message. Experimental.
* `syslog-frame` — Prefixes payload with its length in bytes (for syslog over TCP, see RFC5425 "4.3. Sending Data").


### Replace

`set $field replace <regexp> <replacement> [in <subject>];`
Takes subject string, replaces all occurences of regexp in it with the replacement,
and stores the new value into $field. Default subject is $field itself.

    set $payload replace ~warn(ing)?~i 'WARNING';
    set $host replace ~^www\.~ '' in '$subdomain.$domain';

Be aware of double-escaping in replacement strings! Example:

    set $payload replace ~\n~ '\\\\n';

This command converts newlines into `\n` sequences.


### Date and time

`set $field date ...` and `set $field time ...` support the following options:

    set $field date [as <format>] [in <subject>]
    set $field time [as <format>] [in <subject>]

Available date formats:

* `time` — "11:26:12"
* `date` — "2018-06-13"
* `datetime` — "2018-06-12T11:26:12+00:00".
    This is an ISO 8601 variant with timezone always written like "+00:00" (no "Z").
* `unixtime-seconds` — 1528799172.
* `unixtime-milliseconds` — 1528799172000 (subsecond part may be non-zero).
* `unixtime-microseconds` — 1528799172000000 (subsecond part may be non-zero).
* `unixtime-nanoseconds` — 1528799172000000000 (subsecond part may be non-zero).
* Any format string for java.time.format.DateTimeFormatter.
    `set $field date as 'yyyy-MM-dd HH:mm';`

Beholder will try to preserve timezone information when it can.

    set $date '2018-06-12T11:26:12+01:00';
    set $time time in $date; # 11:26:12 — timezone will be preserved even if local time is not +01:00

Date parser understands some common formats of datetimes. For now a safe bet is to use
ISO 8601 full datetime that Beholder produces by default: "2018-06-12T11:26:12+00:00".


### JSON

`set $payload json` generates a JSON string with all fields of the message.
Alternatively you may specify which fields to include: `set $payload json $field $field2`.
Resulting JSON string will not contain any literal newline characters.

    set $animal cat;
    set $description 'A cat.\nYou should know what a cat is.';
    set $payload json $animal $description;
    # Multiline fields will be encoded with escape sequences:
    # {"animal":"cat","description":"A cat.\nYou should know what a cat is."}
