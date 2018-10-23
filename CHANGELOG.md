# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).
## [0.3.0 2018-10-23]
### Added
- timeout for reading the telnet client. If you don't find what you're looking for, and the timeout expires, whatever has been read will be returned"

## [0.2.0 - 2018-08-03]
### Added
- option to read until one of multiple strings is found
## [0.1.4 - 2018-06-01]
### Fixes
- low socket timeout

## [0.1.3]
### Fixes
- issue where the socket used to test server was left open

## [0.1.1] - 2018-01-07
### Added
- read-all function. A function that will try to read everything the telnet server sends. (probably for repl use only)

[Unreleased]: https://github.com/komcrad/clj-telnet/compare/0.1.1...HEAD
[0.1.1]: https://github.com/komcrad/clj-telnet/compare/0.1.0...0.1.1
