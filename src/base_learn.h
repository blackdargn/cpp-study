//
//  base_learn.cpp
//  cpp-study
//
//  Created by Akon on 15/7/21.
//  Copyright (c) 2015年 Akon. All rights reserved.
//
#include <iostream>

using namespace std;
#pragma pack (4)
namespace learn {
//////////////////////////////////////////////////////////////////////////
// 虚函数
/**
 结论：
    每个声明了虚函数或者继承了虚函数的类，都会有一个自己的vtbl
    同时该类的每个对象都会包含一个vptr去指向该vtbl
    虚函数按照其声明顺序放于vtbl表中, vtbl数组中的每一个元素对应一个函数指针指向该类的虚函数
    如果子类覆盖了父类的虚函数，将被放到了虚表中原来父类虚函数的位置
    在多继承的情况下，每个父类都有自己的虚表。子类的成员函数被放到了第一个父类的表中
 
 虚函数所需的代价:
    调用性能方面:
        在单继承的情况下，调用虚函数所需的代价基本上和非虚函数效率一样 
        虚函数运行时所需的代价主要是虚函数不能是内联函。
    占用空间方面:
        所以虚函数的一个代价就是会增加类的体积
        由于虚函数指针vptr的存在，虚函数也会增加该类的每个对象的体积。
        在单继承或没有继承的情况下，类的每个对象会多一个vptr指针的体积，也就是4个字节；
        在多继承的情况下，类的每个对象会多N个（N＝包含虚函数的父类个数）vptr的体积，也就是4N个字节
 */
class A{
public:
    // vftable vfptr [0]:A::Func1 [1]:A::Func2 [2]A::Func3
    virtual void Func1();
    virtual void Func2();
    virtual void Func3();
    void Func4();
};

class B : public A {
public:
    // A* a = new B
    // vftable vfptr [0]:B::Func1 [1]:A::Func2 [2]A::Func3 [3]B::Func5
    virtual void Func1() override;
    virtual void Func5();
};

class Base1 {
public:
    virtual void Func() {}
    virtual void FuncB1(){}
};

class Base2{
public:
    virtual void Func(){}
    virtual void FuncB2(){}
};

class Base3{
public:
    virtual void Func(){
    }
    virtual void FuncB3(){}
};

class SubClass : public Base1, public Base2, public Base3{
public:
    // vftable1 Sub::Func Base1::FuncB1 Sub::FuncSub
    // vftable2 Sub::Func Base2::FuncB2
    // vftable3 Sub::Func Base3::FuncB3
    virtual void Func() override{}
    virtual void FuncSub(){}
};

//////////////////////////////////////////////////////////////////////////
// 对象模型 类的大小
class X {
    // 大小 ＝ 1， 如果编译器优化没有 对 empty virtual base class 优化
};
class Y : public virtual X {
    // 大小 ＝ 8， 4byte的虚基类指针 和 1byte的X，考虑到 对齐，补齐3byte 共8byte
    // 如果有优化，则为 4
};
class Z : public virtual X {
    // 同 Y
};
class AA : public Y, public Z {
    // Y8 ＋ Z8 ＝ 16
};

void test_class_size(){
    X x;
    Y y;
    Z z;
    AA aa;
    cout<<"size class X = "<<sizeof(x)<<";class Y = "<<sizeof(y)
    <<";class Z = "<<sizeof(z)<<";class AA = "<<sizeof(aa)<<endl;
}
}







