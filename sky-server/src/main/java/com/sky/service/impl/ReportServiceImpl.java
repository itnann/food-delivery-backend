package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end每天范围的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，日期加一
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放没天的营业额数据
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date日期对应的营业额数据， 营业额是指订单为“已完成”的订单总金额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from orders where  order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);


        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end每天范围的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，日期加一
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天新增用户 select count(id) from user where create_time < endTime and create_time > beginTime
        List<Integer> newUserList = new ArrayList<>();
        //存放每天及以前的总用户  select count(id) from user where create_time < endTime
        List<Integer> totalUserList = new ArrayList<>();//select count(id) from user where create_time < endTime
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //向map中添加数据的顺序一定要和动态sql里面的校验顺序一致
            Map map = new HashMap<>();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);
            newUserList.add(totalUser);
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(newUser);

        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end每天范围的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，日期加一
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        //遍历dateList集合，获取每天的订单总数和订单有效数
        for(LocalDate date : dateList){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的订单总数 select count(id) from orders where order_time > beginTime and order_time < endTime
            orderCountList.add(getOrderCount(beginTime, endTime, null));

            //查询每天的有效订单数 select count(id) from orders where order_time > beginTime and order_time < endTime and status = 5
            validOrderCountList.add(getOrderCount(beginTime, endTime, Orders.COMPLETED));

        }
        //计算时间区间内的总订单数量
        Integer orderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Double orderCompletionRate = 0.0;
        if(orderCount != 0){
            orderCompletionRate =(double) validOrderCount / orderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(orderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTIme = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTIme, endTime);

        List<String> nameList = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        /*List<String> nameList = salesTop10.stream().map(x->{
            return x.getName();
        }).collect(Collectors.toList());*/
        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库获得营业数据--查询最近30天的运营数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));


        try(
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
            //3.通过输出流将excel文件输出到客户端
            ServletOutputStream out = response.getOutputStream();
             //基于模版文件创造一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in)){

            //获取表格文件的sheet的标签页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //--填充数据 时间
            XSSFCell cell = sheet.getRow(1).getCell(1);
            XSSFCellStyle cellStyle = cell.getCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cell.setCellValue(begin + "至" + end);
            //获得第四行
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());
            //获得第五行
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7 + i).getCell(1).setCellValue(date.toString());
                sheet.getRow(7 + i).getCell(2).setCellValue(businessData.getTurnover());
                sheet.getRow(7 + i).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet.getRow(7 + i).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet.getRow(7 + i).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet.getRow(7 + i).getCell(6).setCellValue(businessData.getNewUsers());

            }

            //3.通过输出流将excel文件下载到客户端
            excel.write(out);

            /*//关闭资源
            out.close();
            excel.close();
            in.close();*/

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
