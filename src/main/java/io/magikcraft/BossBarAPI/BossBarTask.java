package io.magikcraft.BossBarAPI;

import cn.nukkit.scheduler.PluginTask;

public class BossBarTask extends PluginTask<BossBarPlugin> {

    public BossBarTask(BossBarPlugin owner){
        super(owner);
    }

    @Override
    public void onRun(int currentTick){
        BossBarAPI.updateBossBar();
    }
}