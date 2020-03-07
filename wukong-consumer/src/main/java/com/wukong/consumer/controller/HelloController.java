package com.wukong.consumer.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.wukong.common.model.BaseResult;
import com.wukong.common.model.UserVO;
import com.wukong.consumer.rabbit.fanout.FanoutSender;
import com.wukong.consumer.rabbit.hello.HelloSender;
import com.wukong.consumer.rabbit.many.NeoSender;
import com.wukong.consumer.rabbit.many.NeoSender2;
import com.wukong.consumer.rabbit.object.ObjectSender;
import com.wukong.consumer.rabbit.topic.TopicSender;
import com.wukong.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Reference
    private HelloService helloService;
    @Autowired
    private FanoutSender fanoutSender;
    @Autowired
    private HelloSender helloSender;
    @Autowired
    private NeoSender neoSender;
    @Autowired
    private NeoSender2 neoSender2;
    @Autowired
    private ObjectSender objectSender;
    @Autowired
    private TopicSender topicSender;

    /**
     * 测试dubbo
     * @return
     */
    @RequestMapping(value = "/hello")
    public BaseResult hello() {
        UserVO hello = helloService.sayHello("world");
        return BaseResult.success(hello);
    }

    /**
     * 测试rmq
     * @return
     */
    @RequestMapping(value = "/rmq")
    public BaseResult rmq() {
        fanoutSender.send();
        helloSender.send();
        neoSender.send(1);
        neoSender2.send(2);
        //todo 用stringManager发送的中文乱码
        objectSender.send(helloService.sayHello("ououou"));
        topicSender.send();
        topicSender.send1();
        topicSender.send2();
        return BaseResult.success(null);
    }

}
