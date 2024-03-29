package com.arknights.bot.app.service.impl;


import com.arknights.bot.app.service.GaChaInfoService;
import com.arknights.bot.app.service.GroupsChatService;
import com.arknights.bot.domain.entity.*;
import com.arknights.bot.infra.constant.Constance;
import com.arknights.bot.infra.mapper.GaChaInfoMapper;
import com.arknights.bot.infra.mapper.GroupChatMapper;
import com.arknights.bot.infra.mapper.SkillInfoMapper;
import com.arknights.bot.infra.util.*;
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
import java.math.MathContext;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.arknights.bot.domain.entity.ClassificationEnum.*;
import static com.arknights.bot.infra.util.DesUtil.*;
import static com.arknights.bot.infra.util.RandomMessageUtil.*;
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
    @Resource
    private SkillInfoMapper skillInfoMapper;

    @Autowired
    private SendMsgUtil sendMsgUtil;
    @Autowired
    private RandomMessageUtil randomMessageUtil;

    private final Map<Long, List<Long>> qqMsgRateList = new HashMap<>();
    private final Map<Long, List<Long>> qqRevokeMsgRateList = new HashMap<>();
    private static final String AK_URL = "https://ak.hypergryph.com";
    private static final String TOKEN_URL = "https://web-api.hypergryph.com/account/info/hg";
    private static final String TOKEN_B_URL = "https://web-api.hypergryph.com/account/info/ak-b";


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
            // 菜单格式消息内容标准:#开头
            ClassificationEnum c = SpecialConstanceUtil.GetClass(text);
            log.info("特殊类型消息:{}", c);
            if (text.startsWith(Constance.START_MARK) || !Objects.isNull(c)) {
                String messages = null;
                //判断回复效率，以防和其他机器人互动死锁
                if (getMsgLimit(currentAccount, groupId, name)) {
                    if(Objects.isNull(c)) {
                        // 取 #后的内容
                        messages = text.substring(1);
                    } else {
                        messages = text;
                    }
                    log.info("message内容{}", messages);
                    // 调用菜单关键词匹配方法
                    return queryKeyword(currentAccount, groupId, name, messages);
                }
            }
            // 艾特行为
            if (text.contains(Constance.AT_LOGO)) {
                // 自动回复表情包
                autoEventWithSendPic(groupId, currentAccount, "pic/atPic.jpg");
            }
        }
        return null;
    }

    /**
     * 匹配关键字并进行回复
     *
     * @param qq
     * @param groupId
     * @param name
     * @param text
     * @return
     */
    public String queryKeyword(Long qq, Long groupId, String name, String text) {

        String result = null;
        String resultType = "";
        String attachContent = "";

        ClassificationEnum c = ClassificationUtil.GetClass(text);

        // 技能查询操作
        if (text.startsWith(Constance.SKILL_QUERY)) {
            c = SkillQuery;
            result = text.substring(Constance.SKILL_QUERY.length());
            if (StringUtils.isBlank(result)) {
                log.info("获取干员名异常，正确格式为 #技能 艾雅法拉");
                result = "获取干员技能异常，正确格式为 #技能 艾雅法拉";
                resultType = Constance.TYPE_JUST_TEXT;
            }
        }
        // token特殊操作
        if (text.startsWith(Constance.TOKEN_INSERT)) {
            c = TokenInsert;
            text = text.substring(Constance.TOKEN_INSERT.length());
        }
        // 撅你.gif
        if (text.contains(Constance.FK)) {
            c = FK;
        }
        log.info("当前c:{}", c);
        switch (c) {
            case CaiDan:
                result = "这里是W测试版初号机1.1\n" +
                        "0.获取token方法: #token教程\n" +
                        "1.token录入方法: #token录入{你的token}，例如 #token录入{\"code:0xxxx\n" +
                        "2.寻访查询：#寻访查询\n" +
                        "3.干员技能查询: 例如 #技能 艾雅法拉";
                resultType = Constance.TYPE_JUST_TEXT;
                break;
            case TokenHelp:
                // token获取教程
                if (StringUtils.isNotEmpty(text)) {
                    result = "1.浏览器登录鹰角官网:" + "\n" +
                            AK_URL + "\n" +
                            "2.复制下面链接到浏览器打开:" + "\n" +
                            "官服:" + "\n" +
                            TOKEN_URL + "\n" +
                            "B服:" + "\n" +
                            TOKEN_B_URL + "\n" +
                            "3.复制第2步页面内所有文本内容，添加 #token录入 格式后发送，例如:\n" +
                            "#token录入{\"code\":0,\"data\":{\"content\":\"IkoEX4GFG/Yr1CRGFR1gm7kG\"},\"msg\":\"\"}";
                }
                resultType = Constance.TYPE_JUST_TEXT;
                break;
            case TokenInsert:
                if (StringUtils.isEmpty(text)) {
                    result = "token不能为空，输入格式为 #token录入xxxxx，xxxxx为浏览器复制的内容";
                    break;
                }
                // 对tokenJson解析
                String content = jsonHandler(text);
                if (StringUtils.isEmpty(content)) {
                    result = "token格式异常";
                } else {
                    // token进行加密
                    String encryptString = getEncryptString(content);
                    // token信息插入或更新
                    result = insertOrUpdateToken(encryptString, qq);
                }
                resultType = Constance.TYPE_JUST_TEXT;
                break;
            case GaCha:
                // 寻访记录获取
                // 获取解密后的token
                String token = getToken(qq);
                if (StringUtils.isNotBlank(token)) {
                    String info = gaChaInfoService.gaChaQueryByPage(1, token, qq);
                    if (StringUtils.isEmpty(info)) {
                        result = "当前token无法获取官网信息，请尝试录入新token或切换浏览器";
                    } else {
                        result = info;
                    }
                } else {
                    log.info("token查询为空，请通过 #token获取教程 先获取token，然后通过 #token录入 进行录入 ");
                    result = "token查询为空，请通过 #token获取教程 先获取token，然后通过 #token录入 进行录入 ";
                }
                resultType = Constance.TYPE_JUST_IMG;
                attachContent = Constance.GACHA_LOGO;
                break;
            case RED_ENVELOPE:
                result = "群主速速发红包";
                resultType = Constance.TYPE_JUST_TEXT;
                break;
            case SkillQuery:
                if (!StringUtils.isBlank(result)) {
                    // result内容为干员名
                    attachContent = Constance.SKILL_QUERY;
                    resultType = Constance.TYPE_JUST_IMG;
                }
                break;
            case FK:
                autoEventWithSendPic(groupId, qq, "pic/jue.gif");
                break;
            default:
                if (StringUtils.isBlank(result)) {
                    result = "暂无对应查询，开发中";
                }
                resultType = Constance.TYPE_JUST_TEXT;
        }

        log.info("当前附加内容{}", attachContent);
        log.info("当前resultType:{}", resultType);

        //返回空字符串则不发送信息
        if (StringUtils.isNotBlank(result) && StringUtils.isNotBlank(resultType)) {
            String imageUrl = "";
            log.info("打印result信息:{}", result);
            // 图像消息
            if (Constance.TYPE_JUST_IMG.equals(resultType)) {
                if (Constance.GACHA_LOGO.equals(attachContent)) {
                    // 取img后的内容
                    imageUrl = getGaChaImageUrl(result);
                } else if (Constance.TOKEN_DEMO.equals(attachContent)) {
                    imageUrl = result;
                } else if (Constance.SKILL_QUERY.equals(attachContent)) {
                    imageUrl = getSkillInfoImageUrl(result);
                }
                if (StringUtils.isNotBlank(imageUrl)) {
                    log.info("获取imageUrl成功");
                    // 添加at输出但不附加文本信息
                    sendMsgUtil.CallOPQApiSendImg(groupId, "[ATUSER(" + qq + ")]" + "", SendMsgUtil.picBase64Buf, imageUrl, 2);
                } else {
                    sendMsgUtil.CallOPQApiSendMsg(groupId, "图像生成失败", 2);
                }
            } else if (Constance.TYPE_JUST_TEXT.equals(resultType)) {
                // 纯文字消息
                sendMsgUtil.CallOPQApiSendMsg(groupId, result, 2);
            } else if (StringUtils.isNotBlank(attachContent)) {
                // 文字+图片消息,attachContent为文字等内容
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
            // 同一qq的消息在队列未满三条，直接返回消息
            limit.add(System.currentTimeMillis());
        } else {
            if (getSecondDiff(limit.get(0), second)) {
                //同一队列长度超过三条，但是距离首条消息已经大于10秒
                limit.remove(0);
                // remove操作后元素前移,如果依然超过3条就把多出的时间戳删掉
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

    /**
     * 对于有些喜欢撤回的群友限定一分钟内只记录一次
     *
     * @param qq
     * @param groupId
     * @param name
     * @return
     */
    boolean getMsgLimitForRevoke(Long qq, Long groupId, String name) {
        boolean flag = true;
        // 同一qq号每60秒限制只记录一条(这里只考虑同一群聊内)
        int length = 1;
        int second = 60;
        if (!qqRevokeMsgRateList.containsKey(qq)) {
            List<Long> msgList = new ArrayList<>(1);
            msgList.add(System.currentTimeMillis());
            qqRevokeMsgRateList.put(qq, msgList);
        }
        List<Long> limit = qqRevokeMsgRateList.get(qq);
        if (limit.isEmpty()) {
            // 同一qq的消息在队列中未出现，可以直接返回消息
            limit.add(System.currentTimeMillis());
        } else {
            if (!getSecondDiff(limit.get(0), second)) {
                // 消息间距不到1分钟，不记录
                log.warn("{}连续请求,已忽视撤回消息", name);
                flag = false;
            } else {
                limit.clear();
                limit.add(System.currentTimeMillis());
            }
        }
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
        if (qqRevokeMsgRateList.size() > 256) {
            log.warn("开始对撤回消息速率队列进行回收，当前map长度为：{}", qqRevokeMsgRateList.size());
            //回收所有超过60秒的会话
            qqRevokeMsgRateList.entrySet().removeIf(entry -> getSecondDiff(entry.getValue().get(0), 60));
            log.info("撤回消息速率队列回收结束，当前map长度为：{}", qqRevokeMsgRateList.size());
        }
    }

    /**
     * json串处理
     *
     * @param text
     * @param groupId
     * @return
     * @throws JSONException
     */
    public String jsonHandler(String text, Long groupId) throws JSONException {

        if (text.startsWith("{\"Content")) {
            //这样开头的消息是图片消息,或者艾特操作
            JSONObject jsonObj = new JSONObject(text);
            //提取图片消息中的文字部分，取关键字
            String keyword = jsonObj.getString("Content").split(" ")[0];
            //在json结构前添加关键字信息， 使用波浪线分隔，可以将图片内容和文字内容统一进行处理。
            text = keyword + "\001" + text;
        } else if (text.startsWith("{\"FileID")) {
            // 文件类型
        } else if (text.startsWith("{\"GroupPic")) {
            //纯图片消息，只判断第一张
            JSONObject jsonObj = new JSONObject(text).getJSONArray("GroupPic").getJSONObject(0);
            String Url = jsonObj.getString("Url");
        } else {
            // 纯文字类型
            return text;
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
        String name = message.getNickName();
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
                // 设置撤回消息的回复频率
                if (getMsgLimitForRevoke(qq, groupId, name)) {
                    // 随机数0-6 随机回复消息内容
                    int anInt = getInt(6);
                    String autoMessage = randomMessageUtil.randomReply(anInt);
                    if (anInt == Constance.randomRevokeMaxNum) {
                        sendMsgUtil.CallOPQApiSendImg(groupId, "", SendMsgUtil.picBase64Buf, autoMessage, 2);
                    } else {
                        groupsChatService.sendMessage(groupId, eventData.getLong("UserID"),
                                autoMessage);
                    }
                }
                break;
            default:
                result = "其他事件";
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
        return Objects.nonNull(integer) ? "更新成功" : "更新失败";
    }

    /**
     * 根据qq查询对应token并解密返回
     *
     * @param qq
     * @return
     */
    public String getToken(Long qq) {
        String token = "";
        AccountInfo accountInfo = groupChatMapper.selectAccountInfo(qq);
        if (Objects.isNull(accountInfo)) {
            return "";
        }
        if (StringUtils.isNotBlank(accountInfo.getToken())) {
            try {
                token = getDecryptString(accountInfo.getToken());
            } catch (Exception e) {
                return "";
            }
        }
        return token;
    }

    public void autoEventWithSendPic(Long groupId, Long qq, String path) {
        log.info("调用已有图片返回");
        String formatType = "png";
        if(path.contains("gif")){
            formatType = "gif";
        }
        try {
            // resource获取图片,并InputStream转为BufferedImage,再转为url调用OPQ的api
            ClassPathResource resource = new ClassPathResource(path);
            InputStream inputStream = resource.getInputStream();
            BufferedImage image = ImageIO.read(inputStream);
            String base641 = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image, formatType)));
            sendMsgUtil.CallOPQApiSendImg(groupId, "", SendMsgUtil.picBase64Buf, base641, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getGaChaImageUrl(String result) {
        Long processId = Long.valueOf(result);
        log.info("processId批次号为:{}", processId);
        result = "";
        if (Objects.nonNull(processId)) {
            List<GaChaInfo> gaChaInfoList = gaChaInfoMapper.selectGaChaByProcessId(processId);
            if (CollectionUtils.isEmpty(gaChaInfoList)) {
                return null;
            }
            try {
                // 生成image转url
                result = CallOPQApiSendImgMsg(gaChaInfoList);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
        return "";
    }

    public String getSkillInfoImageUrl(String result) {
        String name = result;
        log.info("当前查询干员为:{}", name);
        result = "";
        if (StringUtils.isNotBlank(name)) {
            List<SkillLevelInfo> skillLevelInfoList = skillInfoMapper.selectSkillInfoByName(name);
            if (CollectionUtils.isEmpty(skillLevelInfoList)) {
                return null;
            }
            try {
                // 生成image转url
                result = CallOPQApiSendSkillImgMsg(skillLevelInfoList);
            } catch (Exception e) {
                e.printStackTrace();
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
        BufferedImage pic = drawPicByGaChaList(gaChaInfoList);
        String s = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(pic, "png")));
        pic = null;
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

        int fourStarOperatorCounts = 0;
        int threeStarOperatorCounts = 0;
        // 当前卡池寻访数
        int poolGaChaCounts = 0;
        // 当次官网查询记录所获寻访次数(十连按10次算)
        int counts = value.size();
        ;

        // 对最近一次官网的寻访记录数据进行分组
        Map<Integer, List<GaChaInfo>> gaChaGroup = value.stream().collect(Collectors.groupingBy(GaChaInfo::getRarity));
        // 统计各星级数量以及按卡池分组
        List<GaChaInfo> topOperatorList = gaChaGroup.get(6);
        List<GaChaInfo> seniorOperatorList = gaChaGroup.get(5);
        List<GaChaInfo> fourStarOperatorList = gaChaGroup.get(4);
        List<GaChaInfo> threeStarOperatorList = gaChaGroup.get(3);
        log.info("六星干员数量{}", topOperatorList);
        log.info("五星干员数量{}", seniorOperatorList);
        log.info("四星干员数量{}", fourStarOperatorList);
        log.info("三星干员数量{}", threeStarOperatorList);

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
        BufferedImage image = new BufferedImage(1250, (height + 1) * 40, BufferedImage.TYPE_INT_BGR);
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
        g.drawString("查询范围是30日内的官网可查100条记录<10连算一条>", 0, 100);

        // part1
        //     * [卡池统计]
        //     * xxxx池 xx发 六星数量，五星数量
        //     * xx池   xx发 六星数量，五星数量
        // 铁青色
        g.setColor(new Color(70, 130, 180));
        g.drawString(part1 + "共计" + counts + "抽", 0, 155);
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
            List<GaChaInfo> fourStarList = gaChaInfoList.stream().filter(e -> e.getRarity() == 4).collect(Collectors.toList());
            List<GaChaInfo> threeStarList = gaChaInfoList.stream().filter(e -> e.getRarity() == 3).collect(Collectors.toList());

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
            if (CollectionUtils.isEmpty(fourStarList)) {
                fourStarOperatorCounts = 0;
            } else {
                fourStarOperatorCounts = fourStarList.size();
            }
            if (CollectionUtils.isEmpty(threeStarList)) {
                threeStarOperatorCounts = 0;
            } else {
                threeStarOperatorCounts = threeStarList.size();
            }

            g.drawString("  " + "[卡池" + poolName + "]共寻访" + poolGaChaCounts + "次", 0, 50 + 50 * length);
            length++;
            g.drawString("  六星:" + topOperatorCounts + "  五星:" + seniorOperatorCounts + "  四星:" + fourStarOperatorCounts + "  三星:" + threeStarOperatorCounts, 0, 50 + 50 * length);
            length++;
        }

        // part2
        // 铁青色
        g.setColor(new Color(70, 130, 180));
        g.drawString(part2, 0, 50 + 50 * length);
        length++;
        g.drawString("最新官网可查寻访的出货记录", 0, 50 + 50 * length);
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


    /**
     * 干员技能信息封装
     *
     * @param skillLevelInfoList
     * @return
     */
    public String CallOPQApiSendSkillImgMsg(List<SkillLevelInfo> skillLevelInfoList) {
        BufferedImage image = drawPicBySkillInfoList(skillLevelInfoList);
        if (Objects.isNull(image)) {
            return null;
        }
        return replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image, "png")));
    }


    /**
     * 画出一个组合的结果
     *
     * @param value
     * @return
     */
    private BufferedImage drawPicBySkillInfoList(List<SkillLevelInfo> value) {

        String zhName = "";
        String enName = "";
        String itemUsage = "";
        String openLevel = "";
        String powerType = "";
        String triggerType = "";
        String skillName = "";

        // 对技能按顺序分组
        Map<Integer, List<SkillLevelInfo>> skillOrderListMap = value.stream().collect(Collectors.groupingBy(SkillLevelInfo::getSkillOrder));
        // 技能1，技能2，技能3
        List<SkillLevelInfo> skillFirstList = skillOrderListMap.get(1);
        List<SkillLevelInfo> skillSecondList = skillOrderListMap.get(2);
        List<SkillLevelInfo> skillThirdList = skillOrderListMap.get(3);

        // 按照技能升序排列
        if (!CollectionUtils.isEmpty(skillFirstList)) {
            skillFirstList = skillFirstList.stream().sorted(Comparator.comparing(SkillLevelInfo::getSkillLevel)).collect(Collectors.toList());
            zhName = skillFirstList.get(0).getZhName();
            enName = skillFirstList.get(0).getEnName();
            itemUsage = skillFirstList.get(0).getItemUsage();

            if (!CollectionUtils.isEmpty(skillSecondList)) {
                skillSecondList = skillSecondList.stream().sorted(Comparator.comparing(SkillLevelInfo::getSkillLevel)).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(skillThirdList)) {
                    skillThirdList = skillThirdList.stream().sorted(Comparator.comparing(SkillLevelInfo::getSkillLevel)).collect(Collectors.toList());
                }
            }
        } else {
            log.error("一技能信息为空");
            return null;
        }
        log.info("一技能list:{}", skillFirstList);
        log.info("二技能list:{}", skillSecondList);
        log.info("三技能list:{}", skillThirdList);

        int height = 90;
        log.info("height:{}", height);

        // 表格上下间距
        int cellHeight = 70;
        // 表格总宽
        int tableWeight = 1600;
        int i = 4;
        BufferedImage image = new BufferedImage(1650, (height + 1) * 40, BufferedImage.TYPE_INT_BGR);
        Font h3Font = new Font("楷体", Font.BOLD, 40);
        Font h2Font = new Font("楷体", Font.BOLD, 45);
        Font h1Font = new Font("楷体", Font.BOLD, 60);
        Font titleFont = new Font("楷体", Font.BOLD, 70);
        Font font = new Font("楷体", Font.BOLD, 22);
        Graphics g = image.getGraphics();
        // 先用白色填充整张图片,也就是背景
        g.setColor(Color.WHITE);
        //画出矩形区域，以便于在矩形区域内写入文字
        g.fillRect(0, 0, 1650, (height + 1) * 40);
        // 设置画笔字体
        g.setFont(h3Font);

        // 干员名称
        g.setColor(Color.BLACK);
        g.drawString(enName, 0, 30);
        g.setFont(titleFont);
        g.drawString(zhName, 0, 110);
        // 黑色
        // 招聘合同词
        g.setFont(font);
        // 金黄色
        g.setColor(new Color(255, 215, 0));
        g.drawString(itemUsage, 0, 160);

        // 技能
        g.setColor(Color.BLACK);
        g.setFont(h1Font);
        g.drawString("技能", 0, 230);
        // 一技能
        g.setFont(h2Font);
        // 猛男粉
        g.setColor(new Color(255, 182, 193));
        powerType = skillFirstList.get(0).getPowerType();
        triggerType = skillFirstList.get(0).getTriggerType();
        skillName = skillFirstList.get(0).getSkillName();
        openLevel = skillFirstList.get(0).getOpenLevel();
        g.drawString("一技能:" + skillName + "  精英" + openLevel + "开放", 0, i * cellHeight);
        g.setColor(new Color(174, 213, 76));
        g.drawString(powerType + "  " + triggerType, 800, i * cellHeight);
        i++;

        g.setColor(Color.BLACK);
        int count = 0;
        g.setFont(font);
        g.drawString("等级", 30, i * cellHeight);
        g.drawString("描述", 150, i * cellHeight);
        g.drawString("初始", 1450, i * cellHeight);
        g.drawString("消耗", 1500, i * cellHeight);
        g.drawString("持续", 1550, i * cellHeight);
        for (SkillLevelInfo firstSkill : skillFirstList) {
            // 绘制线段(单元格)
            // drawdrawLine(x1, y1, x2, y2) 分别代表第一个点的x,y 坐标和 第二个点的x, y坐标
            g.setColor(Color.GRAY);
            g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);
            Long sk = firstSkill.getSkillLevel();
            String skillLevel = "";
            if (sk == 8) {
                skillLevel = Constance.SKILL_SPECIAL_F;
            } else if (sk == 9) {
                skillLevel = Constance.SKILL_SPECIAL_S;
            } else if (sk == 10) {
                skillLevel = Constance.SKILL_SPECIAL_T;
            } else {
                skillLevel = sk.toString();
            }
            g.setColor(Color.BLACK);
            g.drawString(skillLevel, 30, i * cellHeight + 30);
            String desc = firstSkill.getDescription();
            if (desc.length() > 60) {
                g.drawString(desc.substring(0, 60), 150, i * cellHeight + 30);
                if (desc.length() > 120) {
                    g.drawString(desc.substring(60, 120), 150, i * cellHeight + 60);
                    g.drawString(desc.substring(120), 150, i * cellHeight + 90);
                } else {
                    g.drawString(desc.substring(60), 150, i * cellHeight + 60);
                }
            } else {
                g.drawString(desc, 100, i * cellHeight + 30);
            }
            g.drawString(firstSkill.getInitialValue().toString(), 1450, i * cellHeight + 30);
            g.drawString(firstSkill.getConsumeValue().toString(), 1500, i * cellHeight + 30);
            g.drawString(firstSkill.getSpan().toString(), 1550, i * cellHeight + 30);
            i++;
        }
        // 闭合线
        g.setColor(Color.GRAY);
        g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);

        if (!CollectionUtils.isEmpty(skillSecondList)) {
            i++;
            // 二技能
            g.setFont(h2Font);
            // 猛男粉
            g.setColor(new Color(255, 182, 193));
            powerType = skillSecondList.get(0).getPowerType();
            triggerType = skillSecondList.get(0).getTriggerType();
            skillName = skillSecondList.get(0).getSkillName();
            openLevel = skillSecondList.get(0).getOpenLevel();
            g.drawString("二技能:" + skillName + "  精英" + openLevel + "开放", 0, i * cellHeight);
            g.setColor(new Color(174, 213, 76));
            g.drawString(powerType + "  " + triggerType, 800, i * cellHeight);

            i++;
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.drawString("等级", 30, i * cellHeight);
            g.drawString("描述", 150, i * cellHeight);
            g.drawString("初始", 1450, i * cellHeight);
            g.drawString("消耗", 1500, i * cellHeight);
            g.drawString("持续", 1550, i * cellHeight);
            for (SkillLevelInfo skillLevelInfo : skillSecondList) {
                // 绘制线段(单元格)
                // drawdrawLine(x1, y1, x2, y2) 分别代表第一个点的x,y 坐标和 第二个点的x, y坐标
                g.setColor(Color.GRAY);
                g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);
                Long sk = skillLevelInfo.getSkillLevel();
                String skillLevel = "";
                if (sk == 8) {
                    skillLevel = Constance.SKILL_SPECIAL_F;
                } else if (sk == 9) {
                    skillLevel = Constance.SKILL_SPECIAL_S;
                } else if (sk == 10) {
                    skillLevel = Constance.SKILL_SPECIAL_T;
                } else {
                    skillLevel = sk.toString();
                }
                g.setColor(Color.BLACK);
                g.drawString(skillLevel, 30, i * cellHeight + 30);
                String desc = skillLevelInfo.getDescription();
                if (desc.length() > 60) {
                    g.drawString(desc.substring(0, 60), 100, i * cellHeight + 30);
                    if (desc.length() > 120) {
                        g.drawString(desc.substring(60, 120), 100, i * cellHeight + 60);
                        g.drawString(desc.substring(120), 100, i * cellHeight + 90);
                    } else {
                        g.drawString(desc.substring(60), 100, i * cellHeight + 60);
                    }
                } else {
                    g.drawString(desc, 100, i * cellHeight + 30);
                }
                g.drawString(skillLevelInfo.getInitialValue().toString(), 1450, i * cellHeight + 30);
                g.drawString(skillLevelInfo.getConsumeValue().toString(), 1500, i * cellHeight + 30);
                g.drawString(skillLevelInfo.getSpan().toString(), 1550, i * cellHeight + 30);
                i++;
            }
            // 闭合线
            g.setColor(Color.GRAY);
            g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);
        }

        if (!CollectionUtils.isEmpty(skillThirdList)) {
            i++;
            int counts = 0;
            // 三技能
            g.setFont(h2Font);
            // 猛男粉
            g.setColor(new Color(255, 182, 193));
            powerType = skillThirdList.get(0).getPowerType();
            triggerType = skillThirdList.get(0).getTriggerType();
            skillName = skillThirdList.get(0).getSkillName();
            openLevel = skillThirdList.get(0).getOpenLevel();
            g.drawString("三技能:" + skillName + "  精英" + openLevel + "开放", 0, i * cellHeight);
            g.setColor(new Color(174, 213, 76));
            g.drawString(powerType + "  " + triggerType, 800, i * cellHeight);

            i++;
            g.setColor(Color.BLACK);
            g.setFont(font);
            // 三技能有时较长，需要三行
            i = i - 4;
            cellHeight = 82;
            g.drawString("等级", 30, i * cellHeight);
            g.drawString("描述", 150, i * cellHeight);
            g.drawString("初始", 1450, i * cellHeight);
            g.drawString("消耗", 1500, i * cellHeight);
            g.drawString("持续", 1550, i * cellHeight);
            for (SkillLevelInfo skillLevelInfo : skillThirdList) {
                // 绘制线段(单元格)
                // drawdrawLine(x1, y1, x2, y2) 分别代表第一个点的x,y 坐标和 第二个点的x, y坐标
                g.setColor(Color.GRAY);
                g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);

                Long sk = skillLevelInfo.getSkillLevel();
                String skillLevel = "";
                if (sk == 8) {
                    skillLevel = Constance.SKILL_SPECIAL_F;
                } else if (sk == 9) {
                    skillLevel = Constance.SKILL_SPECIAL_S;
                } else if (sk == 10) {
                    skillLevel = Constance.SKILL_SPECIAL_T;
                } else {
                    skillLevel = sk.toString();
                }
                g.setColor(Color.BLACK);
                g.drawString(skillLevel, 30, i * cellHeight + 30);
                String desc = skillLevelInfo.getDescription();
                if (desc.length() > 60) {
                    g.drawString(desc.substring(0, 60), 100, i * cellHeight + 30);
                    if (desc.length() > 120) {
                        g.drawString(desc.substring(60, 120), 100, i * cellHeight + 60);
                        g.drawString(desc.substring(120), 100, i * cellHeight + 90);
                    } else {
                        g.drawString(desc.substring(60), 100, i * cellHeight + 60);
                    }
                } else {
                    g.drawString(desc, 100, i * cellHeight + 30);
                }
                g.drawString(skillLevelInfo.getInitialValue().toString(), 1450, i * cellHeight + 30);
                g.drawString(skillLevelInfo.getConsumeValue().toString(), 1500, i * cellHeight + 30);
                g.drawString(skillLevelInfo.getSpan().toString(), 1550, i * cellHeight + 30);
                i++;
            }
            // 闭合线
            g.setColor(Color.GRAY);
            g.drawLine(0, i * cellHeight + 5, tableWeight, i * cellHeight + 5);
        }

        g.dispose();

        return image;
    }

    String jsonHandler(String tokenJson) {
        com.alibaba.fastjson.JSONObject parse = (com.alibaba.fastjson.JSONObject) com.alibaba.fastjson.JSONObject.parse(tokenJson);
        String code = parse.getString("code");
        if (!Constance.ZERO.equals(code)) {
            return null;
        }
        com.alibaba.fastjson.JSONObject data = parse.getJSONObject("data");
        String tokenContent = data.getString("content");
        if (StringUtils.isEmpty(tokenContent)) {
            return null;
        }
        log.info("tokenJson解析完成");
        return tokenContent;
    }

}
