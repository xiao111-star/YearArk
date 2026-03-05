## 编码规范
1. 后端使用mybatis plus Service层方法，不使用Mapper层代码，也不在mapper写东西
2. 参数校验优先在实体类中进行，若无法进行，则在Service层完成