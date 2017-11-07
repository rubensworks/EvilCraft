Thanks you for taking the time to contribute to this repository.

Issues can be created for bugs, feature suggestions, performance issues, questions or anything else.
Bug reports should follow the provided template when creating a new issue.

Before opening new issues, make sure that the issue does not already exist, so search through all issues (even the closed ones) before opening a new issue.

### Performance issues

If you encounter a performance issue (i.e. server/client lag), make sure to read the following.

You can use the [Sampler mod](https://forum.industrial-craft.net/thread/10820) for _profiling_ your game.
This mod can create `.nps` files, and can help us to see what parts of the game cause performance issues using software such as [VisualVM](https://visualvm.github.io/).
When opening a performance issue, make sure to send us this `.nps` file.

If you are encountering client lag (FPS issues), run this mod in your client.
If you are encountering server lag (TPS issues), run this mod on your server.

When the lag is starting, run `/sampler start` to start profiling, and `/sampler stop` to stop.
Run `/sampler export [your file name]` to export this profiling result to an `.nps` file.

Download Sampler for Minecraft 1.12: http://files.player.to/sampler-1.73.jar