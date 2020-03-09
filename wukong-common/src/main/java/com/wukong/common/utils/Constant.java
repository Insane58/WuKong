package com.wukong.common.utils;

public interface Constant {

    interface RedisKey{
        //商品缓存
        String KEY_GOODS = "wukong_goods";
        //库存缓存
        String KEY_STOCK = "wukong_stock";
        //销量缓存
        String KEY_SALES = "wukong_sales";
    }

    interface Order{
        /**
         * 0新建未支付，1已支付，2已发货，3已收货，4已退款，5已完成
         */
        int STAT_NOT_PAY = 0;
        int STAT_PAY = 1;
        int STAT_OUT = 2;
        int STAT_RECEIVED = 3;
        int STAT_BACK = 4;
        int STAT_DONE = 5;
    }
}