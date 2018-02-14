/*
 * BossBarAPI
 * A plugin by XenialDan aka thebigsmileXD
 * http://github.com/thebigsmileXD/BossBarAPI
 * Sending the Bossbar independ from the Server software
 *
 * Command and some API added by solo5star
 * porting to nukkit by solo5star
 */
package io.magikcraft.BossBarAPI;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.event.player.PlayerPreLoginEvent;
import cn.nukkit.event.player.PlayerTeleportEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.BossEventPacket;
import cn.nukkit.network.protocol.UpdateAttributesPacket;
import cn.nukkit.utils.Config;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.io.File;

public class BossBarPlugin extends PluginBase implements Listener{

    private static BossBarPlugin instance = null;

    public LinkedHashMap<String, Object> hide;

    public HashMap<Player, Vector3> lastMove = new HashMap<Player, Vector3>();

    @Override
    public void onEnable(){
        System.out.println("Enabling BossBar...");
        this.getDataFolder().mkdirs();
        Config config = new Config(new File(this.getDataFolder(), "hide.yml"), Config.YAML);
        this.hide = (LinkedHashMap<String, Object>) config.getAll();

        Config bossBarConfig = new Config(new File(this.getDataFolder(), "bossBar.yml"), Config.YAML);
        LinkedHashMap<String, Object> bossBarData = (LinkedHashMap<String, Object>) bossBarConfig.getAll();
        for(Object obj : bossBarData.values()){
            LinkedHashMap<String, Object> dat = (LinkedHashMap<String, Object>) obj;

            BossBar bossBar = new BossBar((String) dat.get("owner"));
            bossBar.title = (String) dat.get("title");
            bossBar.maxHealth = (int) dat.get("maxHealth");
            bossBar.currentHealth = (int) dat.get("currentHealth");
            bossBar.visible = (boolean) dat.get("visible");
            bossBar.startTime = (int) dat.get("startTime");
            bossBar.endTime = (int) dat.get("endTime");
            bossBar.showRemainTime = (boolean) dat.get("showRemainTime");
            BossBarAPI.registerBossBar(bossBar);
        }

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getNetwork().registerPacket(BossEventPacket.NETWORK_ID, BossEventPacket.class);
        this.getServer().getNetwork().registerPacket(UpdateAttributesPacket.NETWORK_ID, UpdateAttributesPacket.class);
        //this.getServer().getNetwork().registerPacket(SetEntityDataPacket.NETWORK_ID, SetEntityDataPacket.class);

        this.getServer().getScheduler().scheduleRepeatingTask(new BossBarTask(this), 10);
    }

    public static BossBarPlugin getInstance(){
        return BossBarPlugin.instance;
    }

    @Override
    public void onLoad(){
        BossBarPlugin.instance = this;
    }

    @Override
    public void onDisable(){
        this.save();
    }

    public void save(){
        Config config = new Config(new File(this.getDataFolder(), "hide.yml"), Config.YAML);
        config.setAll(this.hide);
        config.save();

        Config bossBarConfig = new Config(new File(this.getDataFolder(), "bossBar.yml"), Config.YAML);
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        for(final BossBar bossBar : BossBarAPI.getAllBossBar().values()){
            LinkedHashMap<String, Object> dat = new LinkedHashMap<String, Object>(){{
                put("owner", bossBar.owner);
                put("title", bossBar.title);
                put("maxHealth", bossBar.maxHealth);
                put("currentHealth", bossBar.currentHealth);
                put("visible", bossBar.visible);
                put("startTime", bossBar.startTime);
                put("endTime", bossBar.endTime);
                put("showRemainTime", bossBar.showRemainTime);
            }};
            data.put(bossBar.owner, dat);
        }
        bossBarConfig.setAll(data);
        bossBarConfig.save();
    }

    public void message(CommandSender sender, String msg){
        sender.sendMessage("§b§o[ 알림 ] §7" + msg);
    }

    @EventHandler
    public void onPreLogin(PlayerPreLoginEvent event){
        Player p = event.getPlayer();
        this.lastMove.put(p, new Vector3(0, 0));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        BossBarAPI.updateBossBarToPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        this.lastMove.remove(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event){
        Player p = event.getPlayer();
        if(this.lastMove.get(p).distance(p) > 20){
            BossBarAPI.updateBossBarToPlayer(p);
            this.lastMove.put(p, new Vector3(p.x, p.y, p.z));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event){
        BossBarAPI.updateBossBarToPlayer(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sd, Command command, String label, String[] args){
        if(command.getName().equals("bossbar")){
            if(args.length == 0){
                args = new String[]{"x"};
            }
            if(!(sd instanceof Player)){
                sd.sendMessage("Bossbar is only available in-game");
                return true;
            }
            Player sender = (Player) sd;
            String name = sender.getName().toLowerCase();
            StringBuilder sb;

            switch(args[0]){
                case "on":
                    if(! this.hide.containsKey(name)){
                        this.message(sender, "The boss bar is already on.");
                        return true;
                    }
                    this.hide.remove(name);
                    BossBarAPI.updateBossBarToPlayer(sender);
                    this.message(sender, "I turned on the boss bar.");
                    return true;

                case "off":
                    if(this.hide.containsKey(name)){
                        this.message(sender, "The boss bar is already off.");
                        return true;
                    }
                    this.hide.put(name, true);
                    BossBarAPI.removeBossBarToPlayer(sender);
                    this.message(sender, "Bossbar turned off.");
                    return true;

                case "create":
                    if(sender.isOp()){
                        if(args.length < 2){
                            this.message(sender, "usage: /bossbar create [title]");
                            return true;
                        }
                        sb = new StringBuilder();
                        for(int i = 1; i < args.length; ++i){
                            sb.append(args[i]);
                            if(i != args.length - 1){
                                sb.append(" ");
                            }
                        }
                        String owner;
                        for(int id = 1; true; ++id){
                            if(BossBarAPI.getBossBar(Integer.toString(id)) == null){
                                owner = Integer.toString(id);
                                break;
                            }
                        }
                        BossBarAPI.registerBossBar(new BossBar(owner, sb.toString()));
                        this.message(sender, "Successfully created BossBar");
                        return true;
                    }

                case "list":
                    if(sender.isOp()){
                        this.message(sender, "====== List of registered boss bars ======");
                        for(BossBar bossBar : BossBarAPI.getAllBossBar().values()){
                            this.message(sender, "id : " + bossBar.getOwner() + ", title : " + bossBar.getTitle());
                        }
                        return true;
                    }

                case "delete":
                    if(sender.isOp()){
                        if(args.length < 2){
                            this.message(sender, "Usage: /bossbar remove [id]");
                            return true;
                        }
                        if(BossBarAPI.unregisterBossBar(args[1])){
                            this.message(sender, "Successfully deleted the boss bar.");
                            return true;
                        }
                        this.message(sender, "There is no boss bar for this id.");
                        return true;
                    }

                case "title":
                    if(sender.isOp()){
                        if(args.length < 3){
                            this.message(sender, "Usage: /bossbar title [id] [title ...]");
                            return true;
                        }
                        BossBar bossBar = BossBarAPI.getBossBar(args[1]);
                        if(bossBar == null){
                            this.message(sender, "There is no boss bar for this id.");
                            return true;
                        }
                        sb = new StringBuilder();
                        for(int i = 2; i < args.length; ++i){
                            sb.append(args[i]);
                            if(i != args.length - 1){
                                sb.append(" ");
                            }
                        }
                        String title = sb.toString();
                        bossBar.setTitle(title);
                        this.message(sender, "Successfully changed title: " + title);
                        return true;
                    }


                case "health":
                    if(sender.isOp()){
                        if(args.length < 3){
                            this.message(sender, "Usage: /bossbar health [id] [percent (1 ~ 100)]");
                            return true;
                        }
                        int percent;
                        try{
                            percent = Integer.parseInt(args[2]);
                        }catch(Exception e){
                            this.message(sender, "Usage: /bossbar health [id] [percent (1 ~ 100)]");
                            return true;
                        }
                        BossBar bossBar = BossBarAPI.getBossBar(args[1]);
                        if(bossBar == null){
                            this.message(sender, "There is no boss bar for this id.");
                            return true;
                        }
                        bossBar.setHealth(bossBar.getMaxHealth() * percent / 100);
                        this.message(sender, "You have successfully changed your health.");
                        return true;
                    }


                case "timer":
                    if(sender.isOp()){
                        if(args.length < 3){
                            this.message(sender, "Usage: /bossbar timer [id] [time (seconds)]");
                            return true;
                        }
                        int sec;
                        try{
                            sec = Integer.parseInt(args[2]);
                        }catch(Exception e){
                            this.message(sender, "Usage: /bossbar timer [id] [time (seconds)]");
                            return true;
                        }
                        BossBar bossBar = BossBarAPI.getBossBar(args[1]);
                        if(bossBar == null){
                            this.message(sender, "There is no boss bar for this id.");
                            return true;
                        }
                        bossBar.setTimer(sec);
                        this.message(sender, "You have successfully set the timer.");
                        return true;
                    }

                default:
                    this.message(sender, "/bossbar [ON / OFF]");
                    if(sender.isOp()){
                        this.message(sender, "/bossbar create [Title] - Creates a boss bar with the title.");
                        this.message(sender, "/bossbar list - View the list of registered boss bars.");
                        this.message(sender, "/bossbar delete [id] - Delete the corresponding boss bar.");
                        this.message(sender, "/bossbar title [id] [title] - Sets the boss title.");
                        this.message(sender, "/bossbar health [id] [percent (0-100)] - Sets the stamina of the boss bar.");
                        this.message(sender, "/bossbar timer [id] [Time (seconds)] - Sets the timer.");
                    }
                    return true;
            }
        }
        return true;
    }
}