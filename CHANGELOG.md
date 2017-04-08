Change Log
==========

Version 1.0.1 *(2017-04-08)*
----------------------------
- Fixed threading bug where default scheduler was created on the thread that the observable was created on, rather than the thread the consumer subscribed on.

Version 1.0   *(2017-03-19)*
----------------------------
- Initial release.