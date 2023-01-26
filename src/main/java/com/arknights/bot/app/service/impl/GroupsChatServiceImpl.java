package com.arknights.bot.app.service.impl;


import com.arknights.bot.app.service.GaChaInfoService;
import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.AccountInfo;
import com.arknights.bot.domain.entity.ClassificationEnum;
import com.arknights.bot.domain.entity.GaChaInfo;
import com.arknights.bot.domain.entity.GroupsEventInfo;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.mapper.GaChaInfoMapper;
import com.arknights.bot.infra.mapper.GroupChatMapper;
import com.arknights.bot.infra.util.ClassificationUtil;
import com.arknights.bot.infra.util.SendMsgUtil;
import com.arknights.bot.infra.util.TextToImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.arknights.bot.domain.entity.ClassificationEnum.TokenInsert;
import static com.arknights.bot.infra.util.DesUtil.*;
import static com.arknights.bot.infra.util.TextToImageUtil.replaceEnter;


/**
 * Created by wangzhen on 2023/1/19 20:32
 *
 * @author 14869
 */
@Slf4j
@Service
public class GroupsChatServiceImpl implements GroupsChatService {

    @Value("${userConfig.loginQq}")
    private Long loginAccount;
    @Autowired
    private GroupsChatService groupsChatService;
    @Autowired
    private GaChaInfoService gaChaInfoService;
    @Resource
    private GroupChatMapper groupChatMapper;
    @Resource
    private GaChaInfoMapper gaChaInfoMapper;

    @Autowired
    private SendMsgUtil sendMsgUtil;

    private final Map<Long, List<Long>> qqMsgRateList = new HashMap<>();


    @Override
    public String generalMessageHandler(GroupsEventInfo groupsEventInfo) {
        log.info("测试输出群聊消息对应实体内容:{}", groupsEventInfo);
        //获取发送消息的群友qq
        Long currentAccount = groupsEventInfo.getQq();
        // ps:不处理代理qq自己发送的消息
        if (!currentAccount.equals(loginAccount)) {
            log.info("当前接受消息内容:{}", groupsEventInfo.getContent());
            // 新增行为记录: 0为接收，1为发送文字，2为发送其他,此处插入type = 0
            //获取群号、昵称、消息
            Long groupId = groupsEventInfo.getGroupId();
            String name = groupsEventInfo.getNickName();
            String text = groupsEventInfo.getContent();
            log.info("消息内容text:{}", text);
            groupsEventInfo = null;
            try {
                // 处理Json数据
                text = jsonHandler(text, groupId);
                log.info("输出json解析后内容{}", text);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 菜单格式消息内容标准:##开头
            if (text.startsWith(Constance.START_MARK)) {
                //判断回复效率，以防和其他机器人互动死锁
                if (getMsgLimit(currentAccount, groupId, name)) {
                    // 取 ##后的内容
                    String messages = text.substring(2);
                    log.info("message内容{}", messages);
                    // 调用菜单关键词匹配方法
                    return queryKeyword(currentAccount, groupId, name, messages);
                }
            }
            // 艾特行为
            if(text.startsWith(Constance.AT_LOGO)){
                // 自动回复表情包
                autoEventForAt(groupId, currentAccount);
            }
        }
        return null;
    }

    /**
     * 匹配关键字并进行回复
     * @param qq
     * @param groupId
     * @param name
     * @param text
     * @return
     */
    public String queryKeyword(Long qq, Long groupId, String name, String text) {

        String result = null;
        ClassificationEnum c = ClassificationUtil.GetClass(text);

        // token特殊操作
        if(text.startsWith(Constance.TOKEN_INSERT)){
            c = TokenInsert;
            text = text.substring(Constance.TOKEN_INSERT.length());
        }
        switch (c) {
            case CaiDan:
                result = "W测试版初号机1.0\n" +
                        "0.获取token方法：##token获取教程\n" +
                        "1.token录入方法：##token录入{你的token}，例如 ##token录入a7JD8jDdi9spp\n" +
                        "2.寻访记录：##寻访记录\n";
                break;
            case TokenHelp:
                // token获取教程
                try {
                    // resource获取图片,并InputStream转为BufferedImage,再转为url调用OPQ的api
                    ClassPathResource resource = new ClassPathResource("pic/demo.jpg");
                    InputStream inputStream = resource.getInputStream();
                    BufferedImage image = ImageIO.read(inputStream);
                    String base641 = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image)));
                    sendMsgUtil.CallOPQApiSendImg(groupId, "[ATUSER(" + qq + ")]" + name, SendMsgUtil.picBase64Buf, base641, 2);
                    result = "";
                    break;
                }catch (Exception e){
                    log.info("获取demo图片异常{}", e);
                    result =  "...获取教程异常，请稍后再试";
                }
                break;
            case TokenInsert:
                if(StringUtils.isEmpty(text)){
                    result = "token不能为空，输入格式为 ##token录入xxxxx，xxxxx为你的token";
                    break;
                }
                // 对token加密
                String encryptString = getEncryptString(text);
                // token信息插入或更新
                result = insertOrUpdateToken(encryptString, qq);
                break;
            case GaCha:
                // 寻访记录获取
                // 获取解密后的token
                String token = getToken(qq);
                if(StringUtils.isNotBlank(token)){
                    // l8jK5JGHe8SN25PI91kgc7xb
                    String info = gaChaInfoService.gaChaQueryByPage(1, token, qq);
                    if(StringUtils.isEmpty(info)){
                        result = "当前token无法获取官网信息，请尝试录入新token";
                    } else {
                        result = Constance.TYPE_JUST_IMG + info;
                    }
                } else {
                    log.info("token查询为空，请通过 ##token获取教程 先获取token，然后通过 ##token录入 进行录入 ");
                    result = "token查询为空，请通过 ##token获取教程 先获取token，然后通过 ##token录入 进行录入 ";
                }
                break;
            default:
                result = "暂无对应查询，开发中";
        }

        // todo 临时添加
        if(text.contains(Constance.RED_ENVELOPE)){
            result = "狗包速速发红包";
        }

        //返回空字符串则不发送信息
        if (StringUtils.isNotBlank(result)) {

            // 图像消息
            if (result.startsWith(Constance.TYPE_JUST_IMG) && !result.contains(Constance.STATUS_ERROR)) {
                // 取img后的内容
                String imageUrl = getImageUrl(result);
                if (imageUrl != null) {
                    // 添加at输出但不附加文本信息
                    sendMsgUtil.CallOPQApiSendImg(groupId, "[ATUSER(" + qq + ")]" + "", SendMsgUtil.picBase64Buf, imageUrl, 2);
                } else {
                    sendMsgUtil.CallOPQApiSendMsg(groupId, "图像生成失败", 2);
                }
            }

            // 文字消息
            if (!result.startsWith(Constance.TYPE_JUST_IMG)) {
                sendMsgUtil.CallOPQApiSendMsg(groupId, result, 2);
            }
        }
        return result;
    }


    /**
     * 消息响应限速避免宕机
     *
     * @param qq
     * @param groupId
     * @param name
     * @return
     */
    boolean getMsgLimit(Long qq, Long groupId, String name) {
        boolean flag = true;
        //每10秒限制三条消息,10秒内超过5条就不再提示
        int length = 3;
        int maxTips = 5;
        int second = 10;
        if (!qqMsgRateList.containsKey(qq)) {
            List<Long> msgList = new ArrayList<>(maxTips);
            msgList.add(System.currentTimeMillis());
            qqMsgRateList.put(qq, msgList);
        }
        List<Long> limit = qqMsgRateList.get(qq);
        if (limit.size() <= length) {
            //队列未满三条，直接返回消息
            limit.add(System.currentTimeMillis());
        } else {
            if (getSecondDiff(limit.get(0), second)) {
                //队列长度超过三条，但是距离首条消息已经大于10秒
                limit.remove(0);
                //把后面两次提示的时间戳删掉
                while (limit.size() > 3) {
                    limit.remove(3);
                }
                limit.add(System.currentTimeMillis());
            } else {
                if (limit.size() <= maxTips) {
                    //队列长度在3~5之间，并且距离首条消息不足10秒，发出提示
                    log.warn("{}超出单人回复速率,{}", name, limit.size());
                    sendMsgUtil.CallOPQApiSendMsg(groupId, name + "说话太快了，要坏掉了，请稍后再试", 2);
                    limit.add(System.currentTimeMillis());
                } else {
                    //队列长度等于5，直接忽略消息
                    log.warn("{}连续请求,已拒绝消息", name);
                }
                flag = false;
            }
        }
        //对队列进行垃圾回收
        gcMsgLimitRate();

        return flag;
    }

    public boolean getSecondDiff(Long timestamp, int second) {
        return (System.currentTimeMillis() - timestamp) / 1000 > second;
    }

    public void gcMsgLimitRate() {
        //大于2048个队列的时候进行垃圾回收,大概占用24k
        if (qqMsgRateList.size() > 256) {
            log.warn("开始对消息速率队列进行回收，当前map长度为：{}", qqMsgRateList.size());
            //回收所有超过30秒的会话
            qqMsgRateList.entrySet().removeIf(entry -> getSecondDiff(entry.getValue().get(0), 30));
            log.info("消息速率队列回收结束，当前map长度为：{}", qqMsgRateList.size());
        }
    }

    /**
     *  json串处理
     * @param text
     * @param groupId
     * @return
     * @throws JSONException
     */
    public String jsonHandler(String text, Long groupId) throws JSONException {
        //这样开头的消息是图片消息,或者艾特操作
        if (text.startsWith("{\"Content")) {
            JSONObject jsonObj = new JSONObject(text);
            //提取图片消息中的文字部分，取关键字
            String keyword = jsonObj.getString("Content").split(" ")[0];
            //在json结构前添加关键字信息， 使用波浪线分隔，可以将图片内容和文字内容统一进行处理。
            text = keyword + "\001" + text;
        } else {
            // 非文本内容暂时用不到
            text = "";
        }
        return text;
    }

    /**
     * 入群事件提醒和撤回消息提醒
     *
     * @param message
     */
    @Override
    public void eventMessageHandler(GroupsEventInfo message) {
        Long qq = message.getQq();
        Long groupId = message.getGroupId();
        log.info("接受到事件消息:{}", message.getContent());
        String type = message.getMsgType();
        String result;
        JSONObject eventData;
        switch (type) {
            case "ON_EVENT_GROUP_JOIN":
                //入群事件
                result = "";
                eventData = new JSONObject(message.getEventData());
                groupsChatService.sendMessage(groupId, eventData.getLong("UserID"),
                        "欢迎" + eventData.getString("UserName") + "入群");
                break;
            case "ON_EVENT_GROUP_REVOKE":
                //撤回消息事件
                result = "";
                eventData = new JSONObject(message.getEventData());
                groupsChatService.sendMessage(groupId, eventData.getLong("UserID"),
                        "谁刚刚撤回了消息,让我看看!!");
                break;
            default:
                result = "";
        }
    }

    @Override
    public void sendMessage(Long groupId, Long qq, String text) {
        sendMsgUtil.CallOPQApiSendMsg(groupId, text, 2);
    }

    @Override
    public String insertOrUpdateToken(String token, Long qq) {

        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setToken(token);
        accountInfo.setQq(qq);
        Integer integer = groupChatMapper.insertAccountInfo(accountInfo);
        return Objects.nonNull(integer)?"更新成功":"更新失败";
    }

    /**
     * 根据qq查询对应token并解密返回
     * @param qq
     * @return
     */
    public String getToken(Long qq){
        String token = "";
        AccountInfo accountInfo = groupChatMapper.selectAccountInfo(qq);
        if(Objects.isNull(accountInfo)){
            return "";
        }
        if(StringUtils.isNotBlank(accountInfo.getToken())){
            try {
                token = getDecryptString(accountInfo.getToken());
            }catch (Exception e){
                return "";
            }
        }
        return token;
    }

    public void autoEventForAt(Long groupId, Long qq){
        try {
            // resource获取图片,并InputStream转为BufferedImage,再转为url调用OPQ的api
            ClassPathResource resource = new ClassPathResource("pic/atPic.jpg");
            InputStream inputStream = resource.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            String base641 = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image)));
            sendMsgUtil.CallOPQApiSendImg(groupId, "", SendMsgUtil.picBase64Buf, base641, 2);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public String getImageUrl(String result){
        Long processId = Long.valueOf(result.substring(3));
        log.info("processId批次号为:{}", processId);
        result = "";
        if (Objects.nonNull(processId)) {
            List<GaChaInfo> gaChaInfoList = gaChaInfoMapper.selectGaChaByProcessId(processId);
            if (CollectionUtils.isEmpty(gaChaInfoList)) {
                return null;
            }
            try {
                // 生成image转url
                result =  CallOPQApiSendImgMsg(gaChaInfoList);
            } catch (Exception e){
                return "";
            }
            return result;
        }
        return "";
    }

    /**
     * 当前批次数据统计，文字转图
     * <p>
     * 图片模板如下：
     * [寻访统计]
     * 寻访范围是30日内的100条数据
     * [卡池统计]
     * xxxx池 xx发 六星数量，五星数量，六星出货占比xx,五星出货占比
     * xx池   xx发 六星数量，五星数量，六星出货占比xx,五星出货占比
     * [高星统计]
     * ★★★★★★
     * 麦迪文 xx卡池
     * 年    xx卡池
     *
     * @param gaChaInfoList
     */
    public String CallOPQApiSendImgMsg(List<GaChaInfo> gaChaInfoList) {

        //保存结果
        int height = 0;
        java.util.List<BufferedImage> imagesList = new ArrayList<>();
        boolean isReturn = false;


        BufferedImage pic = drawPicByGaChaList(gaChaInfoList);

        if (Objects.nonNull(pic)) {
            isReturn = true;
            imagesList.add(pic);
        }

        if (!isReturn) {
            return null;
        }
        int maxHeight = 0;
        for (BufferedImage bf : imagesList) {
            maxHeight += bf.getHeight() + 1;
        }
        BufferedImage image = new BufferedImage(1250, maxHeight + 10, BufferedImage.TYPE_INT_BGR);
        Font font = new Font("楷体", Font.BOLD, 50);
        Graphics g = image.getGraphics();
        // 先用白色填充整张图片,也就是背景
        g.setColor(Color.WHITE);
        // 画出矩形区域，以便于在矩形区域内写入文字
        g.fillRect(0, 0, 1250, maxHeight + 10);
        // 设置画笔字体
        g.setFont(font);
        for (BufferedImage bf : imagesList) {
            if (bf != null) {
                g.drawImage(bf, 0, height, null);
                height += bf.getHeight();
            }
        }
        g.dispose();

        String s = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image)));
        image = null;
        return s;

    }


    /**
     * 画出一个组合的结果
     *
     * @param value
     * @return
     */
    private BufferedImage drawPicByGaChaList(List<GaChaInfo> value) {


        // 根据模板分为三部分填充

        //获取到Key，Value
        String top = "[ 寻访统计 ]";
        String part1 = "[ 卡池统计 ]";
        String part2 = "[ 高星统计 ]";

        // 寻访内六星干员总数
        int topOperatorCounts = 0;
        // 寻访内五星干员总数
        int seniorOperatorCounts = 0;
        // 当前卡池寻访数
        int poolGaChaCounts = 0;

        // 当前寻访数量
        int size = value.size();
        // 对最近100条寻访进行分组
        Map<Integer, List<GaChaInfo>> gaChaGroup = value.stream().collect(Collectors.groupingBy(GaChaInfo::getRarity));
        // 统计六星，五星数量以及按卡池分组
        List<GaChaInfo> topOperatorList = gaChaGroup.get(6);
        List<GaChaInfo> seniorOperatorList = gaChaGroup.get(5);
        log.info("六星干员数量{}", topOperatorList);
        log.info("五星干员数量{}", seniorOperatorList);
        Map<String, GaChaInfo> topGaChaInfoMap;
        Map<String, GaChaInfo> seniorGaChaInfoMap;
        Map<String, List<GaChaInfo>> gaChaInfoPoolsMap;
        if (!CollectionUtils.isEmpty(topOperatorList)) {
            topOperatorCounts = topOperatorList.size();
        }
        if (!CollectionUtils.isEmpty(seniorOperatorList)) {
            seniorOperatorCounts = seniorOperatorList.size();
        }

        // pool作为Map的key
        gaChaInfoPoolsMap = value.stream().collect(Collectors.groupingBy(GaChaInfo::getPool));

        log.info("寻访记录卡池集合" + gaChaInfoPoolsMap.keySet().toString());

        int height = 60;
        log.info("height:{}", height);

        int length = 0;
        BufferedImage image = new BufferedImage(1250, (height + 1) * 50 + 10, BufferedImage.TYPE_INT_BGR);
        Font font = new Font("楷体", Font.BOLD, 50);
        Graphics g = image.getGraphics();
        // 先用白色填充整张图片,也就是背景
        g.setColor(Color.WHITE);
        //画出矩形区域，以便于在矩形区域内写入文字
        g.fillRect(0, 0, 1250, (height + 1) * 50 + 10);
        // 设置画笔字体
        g.setFont(font);

        // top
        // 草绿色
        g.setColor(new Color(174, 213, 76));
        g.drawString(top, 0, 50);
        // 黑色
        g.setColor(Color.BLACK);
        g.drawString("寻访查询范围是30日内的100条记录", 0, 100);

        // part1
        //     * [卡池统计]
        //     * xxxx池 xx发 六星数量，五星数量
        //     * xx池   xx发 六星数量，五星数量
        // 铁青色
        g.setColor(new Color(70, 130, 180));
        g.drawString(part1, 0, 150);
        length = 4;
        g.setColor(Color.BLACK);
        for (Map.Entry<String, List<GaChaInfo>> item : gaChaInfoPoolsMap.entrySet()) {
            // 卡池名
            String poolName = item.getKey();
            // 此卡池寻访获得的干员列表
            List<GaChaInfo> gaChaInfoList = item.getValue();
            poolGaChaCounts = gaChaInfoList.size();
            // 单个卡池五六星干员统计
            List<GaChaInfo> topList = gaChaInfoList.stream().filter(e -> e.getRarity() == 6).collect(Collectors.toList());
            List<GaChaInfo> seniorList = gaChaInfoList.stream().filter(e -> e.getRarity() == 5).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(topList)) {
                topOperatorCounts = 0;
            } else {
                topOperatorCounts = topList.size();
            }
            if (CollectionUtils.isEmpty(seniorList)) {
                seniorOperatorCounts = 0;
            } else {
                seniorOperatorCounts = seniorList.size();
            }

            g.drawString("   " + "[卡池" + poolName + "]共寻访" + poolGaChaCounts + "次", 0, 50 + 50 * length);
            length++;
            g.drawString("  六星数量:" + topOperatorCounts + "  五星数量:" + seniorOperatorCounts, 0, 50 + 50 * length);
            length++;
        }

        // part2
        // 铁青色
        g.setColor(new Color(70, 130, 180));
        g.drawString(part2, 0, 50 + 50 * length);
        length++;
        g.drawString("最近100次寻访的六星五星记录", 0, 50 + 50 * length);
        length++;
        String isNew = "";

        // 六星填充
        if (!topOperatorList.isEmpty()) {
            g.setColor(Color.RED);
            g.drawString("  ★★★★★★", 0, 50 + 50 * length);
            length++;
            g.setColor(Color.BLACK);
            for (GaChaInfo line : topOperatorList) {
                if (Objects.nonNull(line.getIsNew()) && line.getIsNew()) {
                    isNew = "new!";
                } else {
                    isNew = "";
                }
                g.drawString("    " + line.getOperatorsName() + "[" + line.getPool() + "]" + " " + isNew, 0, 50 + 50 * length);
                length++;
            }
        }
        // 五星填充
        if (!seniorOperatorList.isEmpty()) {
            g.setColor(Color.ORANGE);
            g.drawString("  ★★★★★", 0, 50 + 50 * length);
            length++;
            g.setColor(Color.BLACK);
            for (GaChaInfo line : seniorOperatorList) {
                if (Objects.nonNull(line.getIsNew()) && line.getIsNew()) {
                    isNew = "new!";
                } else {
                    isNew = "";
                }
                g.drawString("    " + line.getOperatorsName() + "[" + line.getPool() + "]" + " " + isNew, 0, 50 + 50 * length);
                length++;
            }
        }


        g.dispose();

        return image;
    }

}
