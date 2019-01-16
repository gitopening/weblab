package com.weblab.adt.weblab.imservice.manager;

import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.weblab.adt.weblab.DB.sp.SystemConfigSp;
import com.weblab.adt.weblab.imservice.event.SocketEvent;
import com.weblab.adt.weblab.utils.Logger;
import org.apache.http.Header;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;

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
    // 请求消息服务器地址
    private AsyncHttpClient client = new AsyncHttpClient();

    public static IMSocketManager instance() {
        return inst;
    }

    public IMSocketManager() {
        logger.d("login#creating IMSocketManager");
    }

    /**自身状态 */
    private SocketEvent socketStatus = SocketEvent.NONE;
    /**快速重新连接的时候需要*/
    private  MsgServerAddrsEntity currentMsgAddress = null;
    /**底层socket*/
//    private SocketThread msgServerThread;


    @Override
    public void doOnStart() {
        socketStatus = SocketEvent.NONE;
        logger.d("IMSocketManager#doOnStart");
    }

    @Override
    public void reset() {
        logger.d("IMSocketManager#reset");
    }

    /**
     * 新版本流程如下
     1.客户端通过域名获得login_server的地址
     2.客户端通过login_server获得msg_serv的地址
     3.客户端带着用户名密码对msg_serv进行登录
     4.msg_serv转给db_proxy进行认证（do not care on client）
     5.将认证结果返回给客户端
     */
    public void reqMsgServerAddrs() {
        logger.d("socket#reqMsgServerAddrs.");
        client.setUserAgent("Android-TT");
        client.get(SystemConfigSp.instance().getStrConfig(SystemConfigSp.SysCfgDimension.LOGINSERVER), new BaseJsonHttpResponseHandler(){
            @Override
            public void onSuccess(int i, Header[] headers, String s, Object o) {
                logger.d("socket#req msgAddress onSuccess, response:%s", s);
                MsgServerAddrsEntity msgServer = (MsgServerAddrsEntity) o;
                if(msgServer == null){
                    triggerEvent(SocketEvent.REQ_MSG_SERVER_ADDRS_FAILED);
                    return;
                }
                connectMsgServer(msgServer);
                triggerEvent(SocketEvent.REQ_MSG_SERVER_ADDRS_SUCCESS);
            }

            @Override
            public void onFailure(int i, Header[] headers, Throwable throwable, String responseString, Object o) {
                logger.d("socket#req msgAddress Failure, errorResponse:%s", responseString);
                triggerEvent(SocketEvent.REQ_MSG_SERVER_ADDRS_FAILED);
            }

            @Override
            protected Object parseResponse(String s, boolean b) throws Throwable {
                /*子类需要提供实现，将请求结果解析成需要的类型 异常怎么处理*/
                JSONObject jsonObject = new JSONObject(s);
                MsgServerAddrsEntity msgServerAddrsEntity = onRepLoginServerAddrs(jsonObject);
                return msgServerAddrsEntity;
            }
        });
    }

    /**
     * 实现自身的事件驱动
     * @param event
     */
    public void triggerEvent(SocketEvent event) {
        setSocketStatus(event);
        EventBus.getDefault().postSticky(event);
    }

    /**------------get/set----------------------------*/
    public SocketEvent getSocketStatus() {
        return socketStatus;
    }

    public void setSocketStatus(SocketEvent socketStatus) {
        this.socketStatus = socketStatus;
    }

    /**
     * 与登陆login是强耦合的关系
     */
    private void connectMsgServer(MsgServerAddrsEntity currentMsgAddress) {
        triggerEvent(SocketEvent.CONNECTING_MSG_SERVER);
        this.currentMsgAddress = currentMsgAddress;

        String priorIP = currentMsgAddress.priorIP;
        int port = currentMsgAddress.port;
        logger.i("login#connectMsgServer -> (%s:%d)",priorIP, port);

        //check again,may be unimportance
//        if (msgServerThread != null) {
//            msgServerThread.close();
//            msgServerThread = null;
//        }
//
//        msgServerThread = new SocketThread(priorIP, port,new MsgServerHandler());
//        msgServerThread.start();
    }

    /**----------------------------请求Msg server地址--实体信息--------------------------------------*/
    /**请求返回的数据*/
    private class MsgServerAddrsEntity {
        int code;
        String msg;
        String priorIP;
        String backupIP;
        int port;
        @Override
        public String toString() {
            return "LoginServerAddrsEntity{" +
                    "code=" + code +
                    ", msg='" + msg + '\'' +
                    ", priorIP='" + priorIP + '\'' +
                    ", backupIP='" + backupIP + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    private MsgServerAddrsEntity onRepLoginServerAddrs(JSONObject json)
            throws JSONException {

        logger.d("login#onRepLoginServerAddrs");

        if (json == null) {
            logger.e("login#json is null");
            return null;
        }

        logger.d("login#onRepLoginServerAddrs json:%s", json);

        int code = json.getInt("code");
        if (code != 0) {
            logger.e("login#code is not right:%d, json:%s", code, json);
            return null;
        }

        String priorIP = json.getString("priorIP");
        String backupIP = json.getString("backupIP");
        int port = json.getInt("port");

        if(json.has("msfsPrior"))
        {
            String msfsPrior = json.getString("msfsPrior");
            String msfsBackup = json.getString("msfsBackup");
            if(!TextUtils.isEmpty(msfsPrior))
            {
                SystemConfigSp.instance().setStrConfig(SystemConfigSp.SysCfgDimension.MSFSSERVER,msfsPrior);
            }
            else
            {
                SystemConfigSp.instance().setStrConfig(SystemConfigSp.SysCfgDimension.MSFSSERVER,msfsBackup);
            }
        }

        if(json.has("discovery"))
        {
            String discoveryUrl = json.getString("discovery");
            if(!TextUtils.isEmpty(discoveryUrl))
            {
                SystemConfigSp.instance().init(ctx.getApplicationContext());
                SystemConfigSp.instance().setStrConfig(SystemConfigSp.SysCfgDimension.DISCOVERYURI,discoveryUrl);
            }
        }

        MsgServerAddrsEntity addrsEntity = new MsgServerAddrsEntity();
        addrsEntity.priorIP = priorIP;
        addrsEntity.backupIP = backupIP;
        addrsEntity.port = port;
        logger.d("login#got loginserverAddrsEntity:%s", addrsEntity);
        return addrsEntity;
    }
}
