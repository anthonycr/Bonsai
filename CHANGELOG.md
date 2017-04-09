Change Log
==========

Version 1.1.0 *(2017-04-09)*
----------------------------
- Removed `Schedulers.current()` in favor of `Schedulers.immediate()`. The current scheduler tried to grab the looper for the current thread and execute tasks on it. This was flaky, so it has been removed in favor of the immediate scheduler which executes tasks immediately and synchronously.
- Added `Schedulers.from(Handler)`.

Version 1.0   *(2017-03-19)*
----------------------------
- Initial release.