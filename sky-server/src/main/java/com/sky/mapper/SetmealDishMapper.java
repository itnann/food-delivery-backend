package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3,4)
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量插入套餐菜品关系数据
     * @param setmealDishes
     */
    @AutoFill(value = OperationType.INSERT)
    void saveBatch(List<SetmealDish> setmealDishes);

    /**
     * 根据套餐id删除套餐菜品关系数据
     * @param setmealIds
     */
    void deleteBySetmealId(List<Long> setmealIds);

    /**
     * 根据套餐id查询菜品关系数据
     * @param SetmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{SetmealId}")
    List<SetmealDish> getDishBySetmealId(Long SetmealId);
}
