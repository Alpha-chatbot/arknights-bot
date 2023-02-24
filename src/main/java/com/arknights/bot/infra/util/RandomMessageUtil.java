package com.arknights.bot.infra.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;

import static com.arknights.bot.infra.util.TextToImageUtil.replaceEnter;

/**
 * Created by wangzhen on 2023/2/24 14:30
 *
 * @author 14869
 */
@Slf4j
@Component
public class RandomMessageUtil {

    /**
     * 随机获取UUID（常用于唯一识别，如ID生成）
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取x内的随机一位数（包含x）
     */
    public static int getInt(int x) {
        if (x <= 0) {
            return 0;
        }
        return (int) (Math.random() * (x + 1));
    }

    /**
     * 获取x到y内的随机一位数（包含x,y）
     */
    public static int getInt(int x, int y) {
        if (x <= 0 || y <= 0 || x > y) {
            return 0;
        }
        return (int) (Math.random() * (y - x + 1)) + x;
    }

    /**
     * 随机获取x位字符串（只包含小写字母，常用于区分不同事物）
     */
    public static String getLetterString(int x) {
        if (x <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < x; i++) {
            sb.append((char) (int) (Math.random() * 26 + 97));
        }
        return sb.toString();
    }

    /**
     * 随机获取x位字符串（只包含数字，常用于验证码）
     */
    public static String getNumberString(int x) {
        if (x <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < x; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    public String randomReply(int x) {
        String message = null;
        switch (x) {
            case 0:
                message = "谁刚刚撤回了消息,让我看看!";
                break;
            case 1:
                message = "刚刚有人说请客,但是撤回了";
                break;
            case 2:
                message = "撤回没用，我都看到了";
                break;
            case 3:
                message = "你又撤回了什么见不得人的消息";
                break;
            case 4:
                message = "怀孕了就直说，大家一起帮你想办法,撤回干嘛";
                break;
            case 5:
                message = "他刚刚说要穿女装给我们看,然后撤回了";
                break;
            case 6:
                try {
                    ClassPathResource resource = new ClassPathResource("pic/revoke.jpg");
                    InputStream inputStream = resource.getInputStream();
                    BufferedImage image = ImageIO.read(inputStream);
                    message = replaceEnter(new BASE64Encoder().encode(TextToImageUtil.imageToBytes(image)));
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            default:
                message = "";
        }
        return message;
    }

}
