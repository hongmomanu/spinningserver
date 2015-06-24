

db.createUser(
  {
    user: "jack",
    pwd: "1313",
    roles:
    [
      {
        role: "userAdminAnyDatabase",
        db: "admin"
      }
    ]
  }
)


db.runCommand(
  {
    usersInfo:"jack",
    showPrivileges:true
  }
)








mongo  127.0.0.1/spinningapp -u jack -p 1313





show dbs
show collections




//工厂信息
db.factoryinfo.insert(
   {
      factoryname :"浙江省绍兴市印花纺织厂",
      factoryaddress: "浙江省绍兴市人民中路31号",
      factoryinfo : "印花绣花"
   }
)

//工厂用户
db.factoryuser.insert(
   {
      username : 'jack',
      factoryid:'',
      realname: '沈括',
      tel:'1234433456',
      usertype : 0    //0:boss 1 :办公 2:车间
   }
)

//工厂产品
db.factorygoods.insert(
    {
    factoryid:'',
    goodsname:'xx印花',
    price:'',
    unit:'',
    imgs:[],
    colors:[]
    }

)

//工厂库存
db.factorygoodsnums.insert(
    {
    goodsid:'',
    num:''
    unit:''
    }

)

//客户表
db.customeruser.insert(
   {
      username : 'lucy',
      realname: '韩梅梅',
      tel:'1234433456'
   }
)

--工厂vs客户关联表
db.factorysvscustomers.insert(
  {
        customerid:"551b4cb83b83719a9aba9c01",
        factoryid:"551dfe4dcb4b40507ebc3ba7"
  }
)

--工厂vs工厂关联表
db.factorysvsfactorys.insert(
  {
        factoryid:"551b4cb83b83719a9aba9c01",
        rtime:new Date(),
        rid:"551b4e1d31ad8b836c655377"
  }
)

--消息表维护 (1 factory,0 customer)

db.messages.insert(
   {
	      fromid: "551b4cb83b83719a9aba9c01",
        toid:"551b4e1d31ad8b836c655377",
        fromtype:1,
        totype:1,
        msgtime:new Date(),
        content:"hello jack",
        isread:false
   }
)



--添加申请表 (1,factory ;0 customer)
db.applyfor.insert(
  {
        fromid:"551b4cb83b83719a9aba9c01",
        toid:"551b4e1d31ad8b836c655377",
        applytype:1
  }
)

--工厂客户推荐表(1,factory ;0 customer)

db.recommend.insert(
  {
      factoryid:"551b4e1d31ad8b836c655377",
      customerid:"551dfe4dcb4b40507ebc3ba7",
      fromid:"551b4cb83b83719a9aba9c01",
      rectype:1,
      iscustomearccepted:false,
      isfactoryaccepted:false,
      isreadbycustomer:false,
      isreadbyfactory:false
  }
)




db.userslocation.ensureIndex( { loc : "2dsphere" } )







