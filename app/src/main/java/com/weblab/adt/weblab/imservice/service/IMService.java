package com.weblab.adt.weblab.imservice.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.weblab.adt.weblab.config.SysConstant;
import com.weblab.adt.weblab.imservice.manager.IMSocketManager;
import com.weblab.adt.weblab.utils.Logger;

import de.greenrobot.event.EventBus;

public class IMService extends Service {
    private Logger logger = Logger.getLogger(IMService.class);

    //所有的管理类
    private IMSocketManager socketMgr = IMSocketManager.instance();

    /**binder*/
    private IMServiceBinder binder = new IMServiceBinder();
    public class IMServiceBinder extends Binder {
        public IMService getService() {
            return IMService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //当第一次运行程序时，从IMApplication过来后，第三个执行这里
        logger.i("IMService onBind");
        return binder;
    }

    @Override
    public void onCreate() {
        logger.i("IMService onCreate");
        super.onCreate();
        EventBus.getDefault().register(this, SysConstant.SERVICE_EVENTBUS_PRIORITY);
        /**
         * 设置该服务为前台服务
         */
        startForeground((int) System.currentTimeMillis(), new Notification());
    }

    @Override
    public void onDestroy() {
        logger.i("IMService onDestroy");
        // todo 在onCreate中使用startForeground
        // 在这个地方是否执行 stopForeground呐
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
