# YT-subtracker

About

This is a program to keep track of the change in subscribers for youtube channels.
Given a channel, the program will open up a URL-stream to the channel's "about"-page, where the subscriber 
count is displayed. Using a regex pattern it will retrieve the number of subscribers for that channel at that
moment. Unless the program is on a test-run, it will save retrieved data to a local directory called "channels". 
The directory for saved data will be created if it doesn't already exist.
Each channel is given a file containing information about when the check was made and how many subscribers
the channel had at that moment. For consecutive checks on a channel, the program will also show how many 
subscribers the channel gained or lost since the last check, if any.
Gain/loss of subscribers is shown in overall change as well as rate per minute and second.


Valid commands

$ java SubTracker test <channel name> - dry run on the specified channel. Returns the amount of subscribers 
    for that channel and displays it in the console window, but it will not create any log-files, nor save 
    any data.

$ java SubTracker $auto$ - checks every channel saved to a local text-file named "channels.txt"

$ java SubTracker <channel_1> <channel_2> ... - check every channel given as parameter and saves the data.
