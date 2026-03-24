package com.sky.task;

import com.sky.entity.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sky.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;


    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")//每分钟执行一次
    //@Scheduled(cron = "0/5 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("处理超时订单: {}", LocalDateTime.now());

        //select * from orders where status = ? and order_time < (当前时间 - 15min)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));
        if(ordersList != null && ordersList.size() > 0){
            ordersList.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            });
        }



    }


    /**
     * 处理自动派送订单
     */
    @Scheduled(cron = "0 0 1 * * ? ")//每天凌晨一点执行一次
    public void processDeliveryOrder(){
        log.info("处理自动派送订单: {}", LocalDateTime.now());

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.CONFIRMED, LocalDateTime.now().minusMinutes(60));
        if(ordersList != null && ordersList.size() > 0){
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }

    }

}
