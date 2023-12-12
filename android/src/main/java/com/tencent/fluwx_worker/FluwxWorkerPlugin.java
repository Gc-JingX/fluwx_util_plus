package com.tencent.fluwx_worker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.tencent.wework.api.IWWAPI;
import com.tencent.wework.api.IWWAPIEventHandler;
import com.tencent.wework.api.WWAPIFactory;
import com.tencent.wework.api.model.BaseMessage;
import com.tencent.wework.api.model.WWAuthMessage;
import com.tencent.wework.api.model.WWBaseMessage;
import com.tencent.wework.api.model.WWBaseRespMessage;
import com.tencent.wework.api.model.WWMediaFile;
import com.tencent.wework.api.model.WWMediaImage;
import com.tencent.wework.api.model.WWMediaLink;
import com.tencent.wework.api.model.WWMediaMiniProgram;
import com.tencent.wework.api.model.WWMediaText;
import com.tencent.wework.api.model.WWMediaVideo;
import com.tencent.wework.api.model.WWSimpleRespMessage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;
import android.util.Log;

/**
 * FluwxWorkerPlugin
 */
public class FluwxWorkerPlugin implements FlutterPlugin, MethodCallHandler {
    private MethodChannel channel;
    private static final String APPID = "ww74c20920b6a8fabc";
    private static final String AGENTID = "1000043";
    private static final String SCHEMA = "wwauth74c20920b6a8fabc000043";

    private Context context;
    private IWWAPI iwwapi;
    private String packageName;
    private ApplicationInfo applicationInfo;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "fluwx_util");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        packageName = context.getPackageName();
        applicationInfo = context.getApplicationInfo();

        //注册
        iwwapi = WWAPIFactory.createWWAPI(context);
        iwwapi.registerApp(SCHEMA);
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        HashMap data = (HashMap) call.arguments;
        Log.e("onMethodCall","onMethodCall ..."+call.method);
        switch (call.method) {
            case "sendAuth":
                sendAuthMsg(new WWAuthMessage.Req(), data, baseMessage -> {
                    Log.e("onMethodCall","onMethodCall ..."+baseMessage.toString());
                    if (baseMessage instanceof WWAuthMessage.Resp) {
                        WWAuthMessage.Resp rsp = (WWAuthMessage.Resp) baseMessage;
                        callbackAuth(rsp,data, result);
                    }
                },result);
                break;
            case "registerApp":
                Object schema = call.argument("schema");
                Log.e("onMethodCall","schema ..."+schema);
                if (schema == null || schema.toString().length() == 0){
                    result.error("invalid schema", "are you sure your schema is correct ?",schema);
                    return;
                }
                iwwapi = WWAPIFactory.createWWAPI(context);
                boolean registered = iwwapi.registerApp(schema.toString());

                Log.e("onMethodCall","registered ..."+registered);
                result.success(registered);
                break;
            case "isWeChatInstalled":
                if (iwwapi == null) {
                    result.error("wxapi not configured", "please config  wxapi first", null);
                } else {
                    result.success(iwwapi.isWWAppInstalled());
                }
                break;
            case "shareText":
                sendTextMsg(new WWMediaText(), data, baseMessage -> {
                    if (baseMessage instanceof WWBaseRespMessage) {
                        callback((WWBaseRespMessage) baseMessage, result);
                    }
                });
                break;
            case "shareImage":
                sendFileMsg(new WWMediaImage(), data, baseMessage -> {
                    if (baseMessage instanceof WWBaseRespMessage) {
                        callback((WWBaseRespMessage) baseMessage, result);
                    }
                });
                break;
            case "shareFile":
                sendFileMsg(new WWMediaFile(), data, baseMessage -> {
                    if (baseMessage instanceof WWBaseRespMessage) {
                        callback((WWBaseRespMessage) baseMessage, result);
                    }
                });
                break;
            case "shareVideo":
                sendFileMsg(new WWMediaVideo(), data, baseMessage -> {
                    if (baseMessage instanceof WWBaseRespMessage) {
                        callback((WWBaseRespMessage) baseMessage, result);
                    }
                });
                break;
            case "shareWebPage":
                sendLinkMsg(new WWMediaLink(), data, baseMessage -> {
                    if (baseMessage instanceof WWBaseRespMessage) {
                        callback((WWBaseRespMessage) baseMessage, result);
                    }
                });
                break;
            case "shareMiniProgram":
                sendMiniProgramMsg(new WWMediaMiniProgram(), data, baseMessage -> {
                    if (baseMessage instanceof WWSimpleRespMessage) {
                        callback((WWSimpleRespMessage) baseMessage, result);
                    }
                });
                break;
            default:
                break;
        }
    }

    private void callback(WWBaseRespMessage wwBaseRespMessage, Result result) {
        if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_CANCEL) {
            result.error("" + wwBaseRespMessage.errCode, "cancel", null);
        } else if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_FAIL) {
            result.error("" + wwBaseRespMessage.errCode, "fail", null);
        } else if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_OK) {
            result.success("" + wwBaseRespMessage.errCode);
        }
    }

    private void callbackAuth(WWBaseRespMessage wwBaseRespMessage,HashMap data,  Result result) {
        if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_CANCEL) {
            result.error("" + wwBaseRespMessage.errCode, "cancel", null);
        } else if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_FAIL) {
            result.error("" + wwBaseRespMessage.errCode, "fail", null);
        } else if (wwBaseRespMessage.errCode == WWAuthMessage.ERR_OK) {
            if (wwBaseRespMessage instanceof WWAuthMessage.Resp) {
                Map<String,Object> result2 = new HashMap<>();
                WWAuthMessage.Resp rsp = (WWAuthMessage.Resp) wwBaseRespMessage;
                result2.put("errCode",rsp.errCode);
                result2.put("code",rsp.code);
                result2.put("state",(String) data.get("state"));

                Log.e("onMethodCall","rsp ..."+rsp.code);
                Log.e("onMethodCall","rsp .state.."+rsp.state);
                Log.e("onMethodCall","rsp .(String)  .."+ data.get("state"));
                channel.invokeMethod("onAuthResponse",result2);
            }
        }
    }

    private void sendAuthMsg(WWAuthMessage.Req req, HashMap data, IWWAPIEventHandler iwwapiEventHandler,Result result) {
        req.sch = SCHEMA;
        req.appId = APPID;
        req.agentId = AGENTID;
        req.state = (String) data.get("state");
        boolean ret = iwwapi.sendMessage(req, iwwapiEventHandler);
        result.success(ret);
    }

    private void sendMiniProgramMsg(@NonNull WWMediaMiniProgram wwMediaMiniProgram, @NonNull HashMap data, IWWAPIEventHandler iwwapiEventHandler) {
        wwMediaMiniProgram.username = (String) data.get("username");
        wwMediaMiniProgram.description = (String) data.get("description");
        wwMediaMiniProgram.path = (String) data.get("path");
        wwMediaMiniProgram.title = (String) data.get("title");

        // 图片
        int resId = (int) data.get("resId");
        Bitmap bitmap = ((BitmapDrawable) context.getDrawable(resId)).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream);
        byte[] byteArray = stream.toByteArray();
        if (byteArray.length > 0) {
            wwMediaMiniProgram.hdImageData = byteArray;
        }

        sendMsg(wwMediaMiniProgram, iwwapiEventHandler);
    }

    private void sendLinkMsg(@NonNull WWMediaLink wwMediaLink, @NonNull HashMap data, IWWAPIEventHandler iwwapiEventHandler) {
        wwMediaLink.thumbUrl = (String) data.get("thumbUrl");
        wwMediaLink.webpageUrl = (String) data.get("webpageUrl");
        wwMediaLink.title = (String) data.get("title");
        wwMediaLink.description = (String) data.get("description");
        wwMediaLink.appPkg = packageName;
        wwMediaLink.appName = applicationInfo.name;
        wwMediaLink.appId = APPID;
        wwMediaLink.agentId = AGENTID;
        iwwapi.sendMessage(wwMediaLink, iwwapiEventHandler);
    }

    private void sendTextMsg(@NonNull WWMediaText wwMediaText, @NonNull HashMap data, IWWAPIEventHandler iwwapiEventHandler) {
        String text = (String) data.get("text");
        wwMediaText.text = text;
        sendMsg(wwMediaText, iwwapiEventHandler);
    }

    private void sendFileMsg(@NonNull WWMediaFile wwMediaFile, @NonNull HashMap data, IWWAPIEventHandler iwwapiEventHandler) {
        String fileName = (String) data.get("fileName");
        String filePath = (String) data.get("filePath");
        sendFileMsg(wwMediaFile, filePath, fileName, iwwapiEventHandler);
    }

    private void sendFileMsg(@NonNull WWMediaFile wwMediaFile, String fileName, String filePath, IWWAPIEventHandler iwwapiEventHandler) {
        wwMediaFile.fileName = fileName;
        wwMediaFile.filePath = filePath;
        sendMsg(wwMediaFile, iwwapiEventHandler);
    }

    private void sendMsg(@NonNull WWBaseMessage wwBaseMessage, IWWAPIEventHandler iwwapiEventHandler) {
        wwBaseMessage.appPkg = packageName;
        wwBaseMessage.appName = applicationInfo.name;
        wwBaseMessage.appId = APPID;
        wwBaseMessage.agentId = AGENTID;
        iwwapi.sendMessage(wwBaseMessage, iwwapiEventHandler);
    }
}
