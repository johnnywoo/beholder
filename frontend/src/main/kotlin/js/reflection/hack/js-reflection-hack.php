<?php

set_exception_handler(function($e) {
    fwrite(STDERR, $e->getMessage()."\n");
    exit(1);
});


if (!isset($argv[1])) {
    throw new Exception("Usage: {$argv[0]} <js-file> [<map-file>]");
}
$scriptFile = $argv[1];
$mapFile    = isset($argv[2]) ? $argv[2] : ($scriptFile . '.map'); // TODO take the map name out of the script (//@ sourceMappingURL)

$script = file_get_contents($scriptFile);
$map    = json_decode(file_get_contents($mapFile), true);
// {
//   "version":3,
//   "file":"script.js",
//   "sources":["file:///home/www/.../ReconnectingWebSocket.kt"],
//   "names":[],
//   "mappings":";;mCAuGuB;mCAAA;qCAAA;kDAAA;8BAZiD;;..."
// }

$mappings = parseMappings($map);

$lines = explode("\n", $script);
$replacements = array();
foreach ($lines as $number => $line) {
    $number++; // lines are numbered from 1
    $column = strpos($line, 'jsClass()'); // TODO multiple classes in the same line
    if ($column !== false) {
        $column += 1; // in IDE char positions start from 1
        echo "Detected jsClass() at {$number}:{$column} in compiled source\n";
        $mapping = getMapping($mappings, $number, $column);
        if (!$mapping) {
            throw new Exception("Cannot find mapping for {$number}:{$column} in source map");
        }

        $className = getClassName($mapping['sourceFile'], $mapping['sourceLine'], $mapping['sourceColumn']);
        echo "Class name: {$className} (from {$mapping['sourceFile']})\n";

        $mapping['className'] = $className;
        $replacements[$mapping['compiledLine'] * 1000 + $mapping['compiledColumn']] = $mapping;

        echo "\n";
    }
}

// sorting replacements so we start replacing at the end of the file and do not confuse column numbers
krsort($replacements);
foreach ($replacements as $mapping) {
    $script = replaceClassName($script, $mapping['compiledLine'], $mapping['compiledColumn'], $mapping['className']);
}
file_put_contents($scriptFile, $script);


//
// FUNCTIONS
//

function replaceClassName($contents, $line, $column, $className)
{
    $lines = explode("\n", $contents);
    $lineContent = $lines[$line - 1];
    if (substr($lineContent, $column - 1, strlen('jsClass()')) !== 'jsClass()') {
        throw new Exception('Incorrect mapping');
    }
    $lines[$line - 1] = substr($lineContent, 0, $column - 1)
        . 'new _.js.reflection.hack.JavaScriptClass(_.' . $className . ', "' . $className . '")'
        . substr($lineContent, $column - 1 + strlen('jsClass()'))
    ;
    return join("\n", $lines);
}

function getClassName($file, $line, $column)
{
    $contents = file_get_contents($file);
    $lines = explode("\n", $contents);
    $lineContent = $lines[$line - 1];
    $jsClassCall = substr($lineContent, $column-1);
    if (!preg_match('/^jsClass<([^>]+)>\(\)/', $jsClassCall, $m)) {
        throw new Exception("Cannot find jsClass<T>() call in {$file}:{$line}:{$column}");
    }
    $classSimpleName = $m[1];
    if (strpos($classSimpleName, '.') !== false) {
        throw new Exception("Please do not use dots in jsClass<T>() calls, just import the class: {$file}:{$line}:{$column}");
    }

    $package = '';
    foreach ($lines as $line) {
        if (preg_match('/^\s*package\s+(\S+)\s*$/', $line, $m)) {
            $package = $m[1];
        }
        if (preg_match('/^\s*import\s+(\S+\.([a-zA-Z0-9_]+))\s*$/', $line, $m)) {
            // TODO maybe something better, support aliases, imported packages, etc
            if ($m[2] == $classSimpleName) {
                return $m[1];
            }
        }
    }

    return $package . '.' . $classSimpleName;
}

function getMapping($mappings, $line, $column)
{
    foreach ($mappings as $mapping) {
        if ($mapping['compiledLine'] == $line && $mapping['compiledColumn'] == $column) {
            return $mapping;
        }
    }
    return null;
}

function readValueFromVLQ(&$str)
{
    static $charToIntMap = array(
        'A' => 0,  'B' => 1,  'C' => 2,  'D' => 3,  'E' => 4,  'F' => 5,  'G' => 6,
        'H' => 7,  'I' => 8,  'J' => 9,  'K' => 10, 'L' => 11, 'M' => 12, 'N' => 13,
        'O' => 14, 'P' => 15, 'Q' => 16, 'R' => 17, 'S' => 18, 'T' => 19, 'U' => 20,
        'V' => 21, 'W' => 22, 'X' => 23, 'Y' => 24, 'Z' => 25, 'a' => 26, 'b' => 27,
        'c' => 28, 'd' => 29, 'e' => 30, 'f' => 31, 'g' => 32, 'h' => 33, 'i' => 34,
        'j' => 35, 'k' => 36, 'l' => 37, 'm' => 38, 'n' => 39, 'o' => 40, 'p' => 41,
        'q' => 42, 'r' => 43, 's' => 44, 't' => 45, 'u' => 46, 'v' => 47, 'w' => 48,
        'x' => 49, 'y' => 50, 'z' => 51, '0' => 52, '1' => 53, '2' => 54, '3' => 55,
        '4' => 56, '5' => 57, '6' => 58, '7' => 59, '8' => 60, '9' => 61, '+' => 62,
        '/' => 63,
    );

    $strLen = strlen($str);

    $i      = 0;
    $result = 0;
    $shift  = 0;

    do {
        if ($i >= $strLen) {
            throw new Exception("Expected more digits in base 64 VLQ value.");
        }
        $digit = $charToIntMap[substr($str, $i++, 1)];
        $isContinuation = $digit & 0b100000; // VLQ_CONTINUATION_BIT
        $digit  &= 0b011111; // VLQ_BASE_MASK
        $result += ($digit << $shift);
        $shift  += 5; // VLQ_BASE_SHIFT
    } while ($isContinuation);

    $str = substr($str, $i);

    return fromVLQSigned($result);
}

function fromVLQSigned($aValue)
{
    $isNegative = ($aValue & 1) === 1;
    $shifted = $aValue >> 1;
    return $isNegative ? -$shifted : $shifted;
}

function parseMappings(array $map)
{
    $mappings              = array();
    $compiledLine          = 1;
    $scannedCompiledColumn = 0;
    $scannedSourceLine     = 0;
    $scannedSourceColumn   = 0;
    $scannedSourceIndex    = 0;
    $scannedNameIndex      = 0;

    $str = $map['mappings'];

    while (strlen($str)) {
        $char = substr($str, 0, 1);
        if ($char == ';') {
            $compiledLine++;
            $str = substr($str, 1);
            $scannedCompiledColumn = 0;
        } else if ($char == ',') {
            $str = substr($str, 1);
        } else {
            $mapping = array(
                'compiledLine' => $compiledLine, // in IDE line numbers start from 1, which is already accounted for
            );

            // generated column
            $scannedCompiledColumn += readValueFromVLQ($str);
            $mapping['compiledColumn'] = $scannedCompiledColumn + 1; // in IDE char positions start from 1

            if (strlen($str) && !preg_match('/^[;,]/', $str)) {
                // original source
                $scannedSourceIndex += readValueFromVLQ($str);
                $mapping['sourceFile'] = $map['sources'][$scannedSourceIndex];
                if (strlen($str) === 0 || preg_match('/^[;,]/', $str)) {
                    throw new Exception('Found a source, but no line and column, compiled line ' . $compiledLine);
                }

                // original line
                $scannedSourceLine += readValueFromVLQ($str);
                $mapping['sourceLine'] = $scannedSourceLine + 1; // in IDE line numbers start from 1
                if (strlen($str) === 0 || preg_match('/^[;,]/', $str)) {
                    throw new Exception('Found a source and line, but no column, compiled line ' . $compiledLine);
                }

                // original column
                $scannedSourceColumn += readValueFromVLQ($str);
                $mapping['sourceColumn'] = $scannedSourceColumn + 1; // in IDE char positions start from 1

                if (strlen($str) > 0 && !preg_match('/^[;,]/', $str)) {
                    // original name
                    $scannedNameIndex += readValueFromVLQ($str);
                    if (isset($map['names'][$scannedNameIndex])) {
                        $mapping['name'] = $map['names'][$scannedNameIndex];
                    }
                }
            }

            $generatedMappings[] = $mapping;
        }
    }

    return $generatedMappings;
}
