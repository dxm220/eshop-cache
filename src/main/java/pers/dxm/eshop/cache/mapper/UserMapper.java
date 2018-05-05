package pers.dxm.eshop.cache.mapper;


import pers.dxm.eshop.cache.model.User;

/**
 * Created by douxm on 2018\1\28 0028.
 */
public interface UserMapper {
    public int insertUser(User user);
    public User findUserInfo();

}
