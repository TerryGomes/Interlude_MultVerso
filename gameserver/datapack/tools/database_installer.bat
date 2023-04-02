@echo off

REM ############################################
REM ## You can change here your own DB params ##
REM ############################################
REM MYSQL BIN PATH
set mysqlBinPath=C:\Program Files\MySQL\MySQL Server 5.5\bin

REM LOGINSERVER
set lsuser=root
set lspass=
set lsdb=p_interlude_acis
set lshost=localhost

REM GAMESERVER
set gsuser=root
set gspass=
set gsdb=p_interlude_acis
set gshost=localhost
REM ############################################

set mysqldumpPath="%mysqlBinPath%\mysqldump"
set mysqlPath="%mysqlBinPath%\mysql"

echo.
echo.                        p_interlude_acis database installation
echo.                        __________________________
echo.
echo OPTIONS : (f) full install, it will destroy all (need validation).
echo           (s) skip characters data, it will install only static server tables.
echo.
:asklogin
set loginprompt=x
set /p loginprompt=Installation type: (f) full, (s) skip or (q) quit? 
if /i %loginprompt%==f goto fullinstall
if /i %loginprompt%==s goto lskip
if /i %loginprompt%==q goto end
goto asklogin

REM ############################################
:fullinstall

:validation
set jaja=x
set /p jaja=Are you sure to delete all databases, even characters (y/n) ? 
if /i %jaja%==y goto destruction
if /i %jaja%==n goto lskip
goto validation

:destruction
echo.
echo.
echo Deleting characters-related tables.
%mysqlPath% -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% < full_install.sql
echo Done.
echo.
echo Installing empty character-related tables.
%mysqlPath% -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% < ../sql/accounts.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/account_premium.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/auctions.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/augmentations.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bbs_favorite.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bbs_forum.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bbs_mail.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bbs_post.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bbs_topic.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/bookmarks.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/buffer_schemes.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/buylists.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle_doorupgrade.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle_functions.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle_manor_procure.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle_manor_production.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle_trapupgrade.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_data.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_friends.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_hennas.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_macroses.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_memo.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_quests.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_raid_points.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_recipebook.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_recommends.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_shortcuts.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_skills.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_skills_save.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/character_subclasses.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/characters.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clan_data.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clan_privs.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clan_skills.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clan_subpledges.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clan_wars.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clanhall.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clanhall_flagwar_attackers.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clanhall_flagwar_members.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clanhall_functions.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/clanhall_siege_attackers.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/cursed_weapons.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/fishing_championship.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/games.sql
%mysqlPath% -h %lshost% -u %lsuser% --password=%lspass% -D %lsdb% < ../sql/gameservers.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/grandboss_list.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/heroes_diary.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/heroes.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/items.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/items_on_ground.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/mdt_bets.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/mdt_history.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/mods_wedding.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/offline_trade.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/olympiad_data.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/olympiad_fights.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/olympiad_nobles_eom.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/olympiad_nobles.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/petition.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/petition_message.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/pets.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/rainbowsprings_attacker_list.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/server_memo.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/seven_signs.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/seven_signs_festival.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/seven_signs_status.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/siege_clans.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/vanhalter_spawnlist.sql

echo Done.
echo.

REM ############################################
:lskip
echo Deleting server tables.
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < gs_install.sql
echo Done.
echo.
echo Installing server tables.
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/castle.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/grandboss_data.sql
%mysqlPath% -h %gshost% -u %gsuser% --password=%gspass% -D %gsdb% < ../sql/spawn_data.sql
REM ############################################
:end
echo.
echo Was fast, isn't it ?
pause
