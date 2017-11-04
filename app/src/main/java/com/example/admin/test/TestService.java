package com.example.admin.test;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by admin on 2017/10/23.
 */

public class TestService extends AccessibilityService {
    // 微信的几个包名，判断在哪个界面
    private static final String LAUNCHER = "com.tencent.mm.ui.LauncherUI";

    private String LUCKY_MONEY_RECEIVER = "com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f";

    private String LUCKY_MONEY_DETAIL = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    // 红包
    private AccessibilityNodeInfo rpNode;
    // 是否有打开微信界面

    // 是否点击了开按钮，打开了详情界面
    private boolean isOpenDetail = false;
    // 是否点击了红包
    private boolean isOpenRP = false;
    // 键盘锁的对象
    private KeyguardManager.KeyguardLock kl;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        switch (type){
            // 通知栏
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.i("xyz","通知栏改变");
                List<CharSequence> list = event.getText();
                for (CharSequence sequence:list){
                    String content = sequence.toString();
                    int i = sequence.toString().indexOf("[微信红包]");
                    if (i == -1){
                        break;
                    }
                    if (!TextUtils.isEmpty(content)){
                        // 如果屏幕锁住状态
                        if (isScreenLocked()){
                            Log.i("xyz","屏幕为锁住状态");
                            // 唤醒屏幕
                            wakeupScreen();
                            // 打开微信界面
                            gotoWeChat(event);
                        }else {
                            gotoWeChat(event);
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.i("xyz","窗口改变");
                String className = event.getClassName().toString();
                Log.i("xyz","className = "+className+"   LAUNCHER = "+LAUNCHER);
                // 当前是否是聊天界面
                if (className.equals(LAUNCHER)) {
                    Log.i("xyz", "当前是聊天界面");
                    findStuff();
                }

                Log.i("xyz","LUCKY_MONEY_RECEIVER = "+LUCKY_MONEY_RECEIVER+"  className = "+event.getClassName().toString());
                Log.i("xyz","isOpenRP = "+isOpenRP);
                if (isOpenRP && LUCKY_MONEY_RECEIVER.equals(event.getClassName().toString())){
                    // 如果打开了抢红包页面
                    AccessibilityNodeInfo info = getRootInActiveWindow();
                    if (findOpenBtn(info)){
                        // 如果找到按钮
                        isOpenDetail = true;
                    }else {
                        back2Home();
                    }
                    isOpenRP = false;
                }

                if (isOpenDetail  && LUCKY_MONEY_DETAIL.equals(event.getClassName().toString())){
                    findInDetail(getRootInActiveWindow());
                    isOpenDetail = false;
                    back2Home();
                }
                break;
        }
    }
    // 释放资源
    private void release() {
        if (kl !=null){
            kl.reenableKeyguard();
        }
        rpNode = null;
    }

    // 在红包详情页看抢到多少钱
    private boolean findInDetail(AccessibilityNodeInfo rootInActiveWindow) {
        for (int i = 0; i < rootInActiveWindow.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = rootInActiveWindow.getChild(i);
            if ("android.widget.TextView".equals(nodeInfo.getClassName().toString())){
                if ("元".equals(nodeInfo.getText().toString())){
                    final float money = Float.parseFloat(rootInActiveWindow.getChild(i-1).getText().toString());
                    Log.i("xyz","money = "+money);
                    return true;
                }
            }
            if (findInDetail(nodeInfo)){
                return true;
            }
        }
        return false;
    }

    // 回到系统桌面
    private void back2Home() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private boolean findOpenBtn(AccessibilityNodeInfo info) {
        Log.i("xyz","寻找按钮");
        for (int i = 0; i < info.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = info.getChild(i);
            Log.i("xyz","className = "+nodeInfo.getClassName());
            if ("android.widget.Button".equals(nodeInfo.getClassName())){
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.i("xyz","点击了按钮");
                return true;
            }
            findOpenBtn(nodeInfo);
        }
        return false;
    }

    private void findStuff() {
        Log.i("xyz","开始查找红包");
        AccessibilityNodeInfo info = getRootInActiveWindow();
//        findRP1(info);
        if (findRP(info)){
            isOpenRP = true;
        }
        if (rpNode!=null){
            rpNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    // 在聊天界面中迭代找红包
    private boolean findRP(AccessibilityNodeInfo info) {
        Log.i("xyz","迭代寻找红包");
        for (int i = 0; i < info.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = info.getChild(i);
            if (nodeInfo == null){
                continue;
            }
            if ("android.widget.TextView".equals(nodeInfo.getClassName())){
                Log.i("xyz","找到TextView = "+nodeInfo.getText());
                if (nodeInfo.getText()!=null){
                    if ("微信红包".equals(nodeInfo.getText().toString())){
                        Log.i("xyz","找到红包了");
                        isOpenRP = true;
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i("xyz","点击了红包");
                        return true;
                    }
                }
            }

            if (findRP(nodeInfo)){
                if ("android.widget.LinearLayout".equals(nodeInfo.getClassName())){
                    rpNode = nodeInfo;
                    return true;
                }
            }
        }
        return false;
    }

//    private void findRP1(AccessibilityNodeInfo info){
//        List<AccessibilityNodeInfo>list = info.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ak3");
//        Log.i("xyy","list = " +list.toString() );
//        for (AccessibilityNodeInfo accessibilityNodeInfo : list){
//            if (accessibilityNodeInfo.getText()!=null && accessibilityNodeInfo.getText().toString().contains("微信红包")){
//                while (!accessibilityNodeInfo.isClickable()){
//                    accessibilityNodeInfo = accessibilityNodeInfo.getParent();
//                }
//                Log.i("xyz","找到有红包的群/人");
//                accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//            }
//        }
//    }

    private void gotoWeChat(AccessibilityEvent event) {
        // 如果event为Notification
        if (event.getParcelableData()!=null&& event.getParcelableData() instanceof Notification){
            Notification notification = (Notification) event.getParcelableData();
            // 获取通知的PendingIntent
            PendingIntent pi = notification.contentIntent;
            Log.i("xyz","pi = "+pi);
            try {
                pi.send();
                Log.i("xyz","打开微信");
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
                Log.i("xyz","打开微信失败 error = "+e.toString());
            }
        }

    }

    private void wakeupScreen() {
        // 电源的管理
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wl = manager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.SCREEN_BRIGHT_WAKE_LOCK,"bright");
        wl.acquire(10000);

        // 键盘锁管理
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        kl =  km.newKeyguardLock("unLock");
        kl.disableKeyguard();
        Log.i("xyz","唤醒屏幕");

    }

    public boolean isScreenLocked() {
////        由于isScreenOn（）被弃用，所以不用这种
//        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        return !manager.isScreenOn();

        KeyguardManager manager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        // 如果flag为true，表示有两种状态：a、屏幕是黑的 b、目前正处于解锁状态 。
        //  如果flag为false，表示目前未锁屏
        boolean flag = manager.inKeyguardRestrictedInputMode();
        return flag;
    }


    @Override
    public void onInterrupt() {
        Log.i("xyz","服务中断");
        Toast.makeText(this,"onInterrupt",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        Toast.makeText(this,"onServiceConnected",Toast.LENGTH_SHORT).show();
        Log.i("xyz","服务开启");
        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this,"onUnbind",Toast.LENGTH_SHORT).show();
        Log.i("xyz","服务解除绑定");
        release();
        return super.onUnbind(intent);
    }
}
