package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;
    /**
     * 新增套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        //将数据插入到套餐表中
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        //需要拿到套餐id
        Long id = setmeal.getId();

        //遍历套餐菜品数据，设置套餐id
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(id));
            setmealDishMapper.saveBatch(setmealDishes);
        }



    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐批量删除
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前套餐是否能够被删除 ---是否存在起售中的商品
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus().equals(StatusConstant.ENABLE)){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //删除套餐表中的数据s
        setmealMapper.deleteBatch(ids);
        setmealDishMapper.deleteBySetmealId(ids);
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return
     */
    @Override
    public SetmealVO getById(Long id) {
        //查询套餐数据
        Setmeal setmeal = setmealMapper.getById(id);
        //查询套餐中的菜品数据
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);
        //封装为 VO对象并返回
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        //更新套餐表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //删除套餐菜品表中的菜品数据
        List<Long> ids = new ArrayList<>();
        ids.add(setmealDTO.getId());
        setmealDishMapper.deleteBySetmealId(ids);

        //插入套餐菜品表中菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        log.info("setmealDishes:{}", setmealDishes);
        if (setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDTO.getId()));
            setmealDishMapper.saveBatch(setmealDishes);
        }
    }

    /**
     * 启用停用套餐
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        //启用：如果套餐下菜品有禁用则无法启用
        if (status.equals(StatusConstant.ENABLE)) {
            //根据套餐id查询套餐菜品数据
            List<Dish> dishes = dishMapper.getDishBySetmealId(id);
            if(dishes != null && dishes.size() > 0){
                dishes.forEach(dish ->{
                    if (dish.getStatus().equals(StatusConstant.DISABLE)){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });}
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
