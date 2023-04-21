##############################################
GEODATA COMPENDIUM
##############################################

Comprehensive guide for geodata, by Tryskell and Hasha.

I	- Supported types of geodata, pros/cons
II	- How to configure it
		a - Prerequisites
		b - Make it work
		c - L2D format
III	- Addendum

##############################################
I - Supported types of geodata, pros/cons
##############################################

* L2OFF:
	The original file system, holding movement information for each block (NSWE flags).
	Try to use it by default.
* L2J:
	All L2J files are based on L2OFF, the main advantage is a slight decrease in file weight (-15%).
	Doesn't worth to be used, unless you have a good reason to use them (e.g. use an heavily handmade/customized files).
* L2D:
	A unique format made by Hasha, holding additional diagonal movement information in addition to regular NSWE flags.
	Heavier file weight (+30%), but the pathfinding algorithms are processed way faster (-35% calculation times).
	Use it, if you plan to not update your geodata files often, since it requires conversion (see below).

##############################################
II - How to configure it
##############################################

----------------------------------------------
a - Prerequisites
----------------------------------------------

* A 64bits Windows/Java JDK is a must-have to run server with geodata. Linux servers don't have the issue.
* The server can start (hardly) with -Xmx1800m. -Xmx2g is recommended.

----------------------------------------------
b - Make it work
----------------------------------------------

To make geodata working:
* unpack your geodata files into "/data/geodata" folder
* open "/config/geoengine.properties" with your favorite text editor and then edit following configs:
	- GeoData = 2
	- GeoDataFormat = the one you have choosen
	- CoordSynchronize = 2
* [optional] scroll down to the bottom and quote the blocks, which you don't want to load (e.g. custom servers with certain areas only)

----------------------------------------------
c - L2D format
----------------------------------------------

* aCis introduces a new geodata file format, named L2D.
* L2D files can be converted from L2OFF/L2J formats without losing any informations.
* Converter is part of the gameserver and conversion is performed according to the "/config/geoengine.properties" and files listed inside.
* Keep in mind to convert new geodata files, once you update your L2OFF/L2J ones.

##############################################
III - Addendum
##############################################

* A map named "Interlude-real-geodata.jpg", updated by RooT, is shared on "/data/geodata" folder.
* It shows all block names, blocks in red aren't supported by L2 client and don't exist as L2OFF.
* You can use that map to easily track blocks.
