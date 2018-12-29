package com.weblab.adt.weblab.imservice.manager;

import com.weblab.adt.weblab.imservice.event.SocketEvent;
import com.weblab.adt.weblab.utils.Logger;

/**
 * @author : yingmu on 14-12-30.
 * @email : yingmu@mogujie.com.
 *
 * 业务层面:
 * 长连接建立成功之后，就要发送登陆信息，否则15s之内就会断开
 * 所以connMsg 与 login是强耦合的关系
 */
public class IMSocketManager extends IMManager {

    private Logger logger = Logger.getLogger(IMSocketManager.class);
    private static IMSocketManager inst = new IMSocketManager();

    public static IMSocketManager instance() {
        return inst;
    }

    public IMSocketManager() {
        logger.d("login#creating IMSocketManager");
    }

    /**自身状态 */
    private SocketEvent socketStatus = SocketEvent.NONE;


    @Override
    public void doOnStart() {
        socketStatus = SocketEvent.NONE;
        logger.d("IMSocketManager#doOnStart");
    }

    @Override
    public void reset() {
        logger.d("IMSocketManager#reset");
    }
}
