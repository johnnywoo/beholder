# Settings

Global settings may be provided at the top level of the config.
It is done in form of special commands.

## Time settings

When Beholder creates a datetime (e.g. when a new message is received), it uses system timezone.
You can override this by setting `create_dates_in_timezone` to a desired timezone.
See [java.time.ZoneId.of() docs](https://docs.oracle.com/javase/9/docs/api/java/time/ZoneId.html#of-java.lang.String-) for details.
Example timezones: `UTC`, `Europe/Moscow`.
When a date is received in a message (not created), Beholder will try to keep its timezone intact.

    create_dates_in_timezone UTC;

## Buffers and queues

When messages cannot be processed quick enough, they are stored in queues.
When the config is being reloaded, incoming messages are stored in from-queues.
When a destination (e.g. a TCP server) is unavailable, outgoing payloads are stored in to-queues.

    queue_chunk_messages 500;

A queue will group messages into chunks. When there are a lot of chunks in a queue, they get stored in a buffer.
Currently there is only one buffer for all this data: the default buffer. Buffer size can be changed using
`memory_bytes` option. You can specify a number of bytes or use a suffix: `k`, `m`, `g`.

    buffer {
        memory_bytes 128m;
        memory_compression lz4-fast; # One of: lz4-fast, off
    }

The `buffer` command can only be used at the top level of the config (not inside `flow` or anywhere else).

When a queue chunk is buffered, it is compressed. You can turn off compression with `memory_compression off` 
inside `buffer` command.

Note: `memory_bytes` is NOT the total memory limit of Beholder. In addition to unbuffered messages in queues,
there are lots of things on JVM that eat bytes. Watch your metrics!
