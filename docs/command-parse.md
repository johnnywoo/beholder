# `parse` — populates message fields according to some format

    parse [keep-unparsed] syslog;
    parse [keep-unparsed] json;
    parse [keep-unparsed] fieldpack;
    parse [keep-unparsed] ~regexp-with-named-groups~;
    parse each-field-as-message;
    parse beholder-stats;

This command sets fields on messages according to chosen format.
If a message cannot be parsed, by default it will be dropped.
If `keep-unparsed` option is specified, unparsed messages will be kept unchanged.

Format `syslog`: syslog variants currently supported are
BSD-style syslog format as produced by nginx (with or without hostname),
and IETF-style syslog without the structured-data section.

Incoming messages look like either of these:

    <190>Nov 25 13:46:44 host nginx: payload
    <190>Nov 25 13:46:44 nginx: payload
    <15>1 2018-04-27T17:49:03+03:00 hostname program 73938 - - payload

Fields produced by `parse syslog`:

* `$date`      — date in ISO format, only from ietf-syslog
* `$facility`  — numeric syslog facility
* `$severity`  — numeric syslog severity
* `$host`      — source host from the message
* `$pid`       — process id
* `$messageId` — message id from ietf-syslog
* `$program`   — program name (nginx calls this "tag")
* `$payload`   — actual log message (this would've been written to a file by nginx)

Format `json`: parses `$payload` as a JSON object and sets its properties as message fields.
The JSON object may only contain numbers, strings, booleans and nulls (no nested objects or arrays).
Boolean values are converted to strings 'true' and 'false'.

Format `fieldpack`: parses `$payload` as a Fieldpack packet which may contain multiple messages.
Experimental.

Format `each-field-as-message`: every field of the message becomes a separate new message.
Original message is dropped. Instead, new messages are produced with fields `$key` and `$value`.
Fields for new messages are taken in unpredictable order.

Format `~regexp-with-named-groups~`: if the regexp matches, named groups from it
become message fields. Group names should not be prefixed with $.

    parse ~(?<logKind>access|error)~;

This will produce field `$logKind` with either 'access' or 'error' as value,
if either word occurs in `$payload`. If both words are present, earliest match is used.

Format `beholder-stats`: fills the message with internal Beholder stats.
Use this with `from timer` to create a health log.

Fields produced by `parse beholder-stats`:

* `$allBuffersAllocatedBytes`     — Total amount of bytes added to all buffers (does not decrease when memory is released)
* `$allBuffersMaxBytes`           — Maximum buffer size, total for all buffers
* `$compressAfterTotalBytes`      — Total size of all data fed into compressors
* `$compressBeforeTotalBytes`     — Total size of data produced by compressors
* `$compressCount`                — Number of compress operations (chunk moves from queue to buffer)
* `$compressDurationMaxNanos`     — Max duration of a compress
* `$compressDurationTotalNanos`   — Total duration of all compress operations
* `$configReloads`                — Number of successful config reloads
* `$decompressCount`              — Number of decompress operations (chunk moves from buffer to queue)
* `$decompressDurationMaxNanos`   — Max duration of a decompress
* `$decompressDurationTotalNanos` — Total duration of all decompress operations
* `$defaultBufferAllocatedBytes`  — Total amount of bytes added to the default buffer (does not decrease when memory is released)
* `$defaultBufferMaxBytes`        — Maximum size of the default buffer
* `$fromTcpMaxBytes`              — Maximum length of a message received over TCP
* `$fromTcpMessages`              — Number of messages received over TCP
* `$fromTcpNewConnections`        — Number of accepted TCP connections
* `$fromTcpTotalBytes`            — Total number of bytes received over TCP
* `$fromUdpMaxBytes`              — Maximum length of a packet received over UDP
* `$fromUdpMessages`              — Number of messages received over UDP
* `$fromUdpTotalBytes`            — Summed length of all packets received over UDP
* `$toTcpMaxBytes`                — Maximum length of a message sent over TCP
* `$toTcpMessages`                — Number of messages sent over TCP
* `$toTcpTotalBytes`              — Total number of bytes sent over TCP
* `$toUdpMaxBytes`                — Maximum length of a packet sent over UDP
* `$toUdpMessages`                — Number of messages sent over UDP
* `$toUdpTotalBytes`              — Summed length of all packets sent over UDP
* `$toFileMaxBytes`               — Maximum length that was written into a file
* `$toFileMessages`               — Number of messages written into files
* `$toFileTotalBytes`             — Summed length of all messages written into files
* `$toShellMaxBytes`              — Maximum length that was written into a shell command
* `$toShellMessages`              — Number of messages written into shell commands
* `$toShellTotalBytes`            — Summed length of all messages written into shell commands
* `$heapBytes`                    — Current heap size in bytes (memory usage)
* `$heapMaxBytes`                 — Maximal heap size
* `$heapUsedBytes`                — Used memory in the heap
* `$messagesReceived`             — Count of received messages
* `$messagesSent`                 — Count of sent messages
* `$packCount`                    — Number of pack operations (chunk moves from queue to buffer)
* `$packDurationMaxNanos`         — Max duration of a pack
* `$packDurationTotalNanos`       — Total duration of all pack operations
* `$queueChunksCreated`           — Number of queue chunks created
* `$queueMaxSize`                 — Maximum size of a queue
* `$queueOverflows`               — Number of messages dropped due to a queue overflow
* `$unpackCount`                  — Number of unpacks (chunk moves from buffer to queue)
* `$unpackDurationMaxNanos`       — Max duration of an unpack
* `$unpackDurationTotalNanos`     — Total duration of all unpacks
* `$unparsedDropped`              — Number of messages dropped due to parse errors
* `$uptimeSeconds`                — Uptime in seconds
* `$payload`                      — A summary of all these stats
* `$influxLineProtocolPayload`    — An InfluxDB line protocol payload with all stats

All counter stats are rotated: numbers are reset to 0 every time `parse beholder-stats` happens.
If you have multiple `parse beholder-stats` commands in your config,
they will have their own counters so they don't reset each other.

`$influxLineProtocolPayload` will also take fields without whitespace in values and convert those to Influx measurement tags.
This field will have a value like this: `beholder,tag=value,tag=value uptimeSeconds=0,... 1536478714733216000`

An easy way to monitor Beholder status is to feed the stats into InfluxDB and then visualize those with e.g. Grafana.

    flow {
        from timer 30 seconds;

        parse beholder-stats;
        parse each-field-as-message;

        switch $value {
            case ~^[0-9]+$~ {}
            default {drop}
        }
        set $host host;
        set $payload 'beholder,host=$host,tag=tagval $key=$value';

        to udp influxdb-host:8089;
    }
