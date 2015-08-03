//============================================================================
// Name        : Study.cpp
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <stdlib.h>
#include <stdio.h>

#include <iostream>
#include <sstream>
#include <thread>
#include <mutex>
#include <future>
#include <condition_variable>
#include <stdexcept>
#include <exception>
#include <chrono>
#include <functional>
#include <atomic>
#include <utility>
#include <vector>
#include <string>
////////////////////////////////////////////////////////////////////////////
// refencen
void fv(int p){
	printf("\n%x", &p);
	printf("\n%x", p);
	p = 0xff;
}
void fr(int &p){
	printf("\n%x", &p);
	printf("\n%x", p);
	p = 0xff;
}
void test_pass_param(){
	int a=0x10;
	printf("--->pass value:\n%x",&a);
	printf("\n%x\n",a);
	fv(a);
	printf("\n%x\n",a);

	printf("--->pass &value:\n%x",&a);
	printf("\n%x\n",a);
	fr(a);
	printf("\n%x\n",a);
}
////////////////////////////////////////////////////////////////////////////
// move
bool is_r_value(int&& v) {
	return true;
}
bool is_r_value(const int &) {
	return false;
}
void test_rv(int && i) {
	std::cout << "is_r_value(i) = " << is_r_value(i) << "\n";
	std::cout << "is_r_value(move<int&>(i)) = " << is_r_value(std::move<int&>(i)) << "\n";
}
void test_rv() {
	std::string str = "Hello";
	std::vector<std::string> v;

	// uses the push_back(const T&) overload, which means
	// we'll incur the cost of copying str
	v.push_back(str);
	std::cout << "After copy, str is \"" << str << "\"\n";

	// uses the rvalue reference push_back(T&&) overload,
	// which means no strings will be copied; instead, the contents
	// of str will be moved into the vector.  This is less
	// expensive, but also means str might now be empty.
	v.push_back(std::move(str));
	std::cout << "After move, str is \"" << str << "\"\n";

	std::cout << "The contents of the vector are \"" << v[0] << "\", \"" << v[1]<< "\"\n";

	// string move assignment operator is often implemented as swap,
	// in this case, the moved-from object is NOT empty
	std::string str2 = "Good-bye";
	std::cout << "Before move from str2, str2 = '" << str2 << "'\n";
	v[0] = std::move(str2);
	std::cout << "After move from str2, str2 = '" << str2 << "'\n";

	std::cout << "The contents of the vector are \"" << v[0] << "\", \"" << v[1]<< "\"\n";
}
////////////////////////////////////////////////////////////////////////////
// constexpr
constexpr int getFive(){
	return 5;
}
int values[getFive() + 5] = {0};
////////////////////////////////////////////////////////////////////////////
// thread
void thread_task() {
    std::cout << "hello thread" << std::endl;
}
void test_thread(){
	std::thread t(thread_task);
	t.join();
}
void f1(int n){
	for(int i =0; i <5; ++i){
		std::cout<<"thread-"<<n<<"-excuting"<<std::endl;
		std::this_thread::sleep_for(std::chrono::milliseconds(10000));
	}
}
void f2(int& n){
	for(int i =0; i <5; ++i){
		std::cout<<"thread-22-excuting = "<<n<<std::endl;
		++n;
		std::this_thread::sleep_for(std::chrono::milliseconds(10000));
	}
}
void test_thread2(){
	int n =0 ;
	// t1 is not a thread
	std::thread t1;
	// t2 pass by value
	std::thread t2(f1, n+1);
	// t3 pass by reference
	std::thread t3(f2, std::ref(n));
	// t4 is now running f2(). t3 is no longer a thread
	std::thread t4(std::move(t3));

	t2.join();
	t4.join();
	std::cout << "Final value of n is " << n << '\n';
}

void thread_task2(int n) {
    std::this_thread::sleep_for(std::chrono::seconds(n));
    std::cout << "hello thread "
        << std::this_thread::get_id()
        << " paused " << n << " seconds" << std::endl;
}
void test_thread3(){
		std::thread threads[5];

	    std::cout << "Spawning 5 threads...\n";
	    for (int i = 0; i < 5; i++) {
	        threads[i] = std::thread(thread_task2, i + 1);
	     }
	    std::cout << "Done spawning threads! Now wait for them to join\n";
	    for (auto& t: threads) {
	        t.join();
	     }
	    std::cout << "All threads joined.\n";
}
////////////////////////////////////////////////////////////////////////////
// mutex
volatile int counter(0);// non-atomic counter
std::mutex mtx;// locks access to counter

void attmp_10k_increase(){
	for(int i =0; i<10000; ++i){
		  // only increase if currently not locked:
		  if(mtx.try_lock()){
			  	  ++counter;
			  	mtx.unlock();
		  }
	}
}

void test_thread_mutex(){
	std::thread threads[10];

	for(int i = 0; i<10; ++i){
		threads[i] = std::thread(attmp_10k_increase);
	}
	for(auto& one: threads){
		one.join();
	}
	std::cout << counter << " successful increases of the counter.\n";
}

std::timed_mutex tmtx;
void fireworks(int n){
	// waiting to get a lock: each thread prints "-" every 200ms:
	while( !tmtx.try_lock_for(std::chrono::milliseconds(500))){
		std::cout<<"-";
	}
	// got a lock! - wait for 1s, then this thread prints "*"
	std::this_thread::sleep_for(std::chrono::milliseconds(200));
	std::cout<<"\n*"<<n<<"*\n";
	tmtx.unlock();
}
void test_timed_mutx(){
	 std::thread threads[10];
	  // spawn 10 threads:
	  for (int i=0; i<10; ++i){
	    threads[i] = std::thread(fireworks, i);
	  }
	  for (auto& th : threads) th.join();
}

std::mutex g_mtx;
void print_even(int n){
	if(n % 2 == 0)  std::cout << n << " is even\n";
	else throw std::logic_error(n + " not even");
}
void print_thread_id (int id) {
	try{
			// using a local lock_guard to lock mtx guarantees unlocking on destruction / exception:
			std::lock_guard<std::mutex> lck(g_mtx);
			print_even(id);
	}catch(std::logic_error& err){
		std::cout << "[exception caught]"<< err.what()<<"\n";
	}
}
void test_lock_guard(){
		std::thread threads[10];
	    // spawn 10 threads:
	    for (int i=0; i<10; ++i){
	        threads[i] = std::thread(print_thread_id,i+1);
	     }
	    for (auto& th : threads) th.join();
}

std::mutex u_mtx;
void print_block(int n, char c){
	// critical section (exclusive access to std::cout signaled by lifetime of lck):
	std::unique_lock<std::mutex> utx(u_mtx);
//	std::lock_guard<std::mutex> utx(u_mtx);
	for(int i =0; i < n; ++i){
		std::cout<<c;
	}
	std::cout << '\n';
}
void test_uniq_lock(){
	std::thread t1(print_block, 50, '*');
	std::thread t2(print_block, 50, '#');

	t1.join();
	t2.join();
}
////////////////////////////////////////////////////////////////////////////
// future
void print_int(std::future<int>& fut){
	int x = fut.get(); // 获取共享状态的值.
	 std::cout << "value: " << x << '\n'; // 打印 value: 10.
}
void test_future(){
	std::promise<int> prom;// 生成一个 std::promise<int> 对象.
	std::future<int> fut = prom.get_future();// 和 future 关联.
	std::thread t1(print_int, std::ref(fut));// 将 future 交给另外一个线程t.
	prom.set_value(10);// 设置共享状态的值, 此处和线程t保持同步.
	t1.join();
}

std::promise<int> prom;
void print_global_promise () {
    std::future<int> fut = prom.get_future();
    int x = fut.get();
    std::cout << "value: " << x << '\n';
}
void test_promise_move(){
	std::thread t1(print_global_promise);
	prom.set_value(10);
	t1.join();

	// prom 被move赋值为一个新的 promise 对象.
	prom = std::promise<int>();

	std::thread t2(print_global_promise);
	prom.set_value(20);
	t2.join();
}

void get_int(std::promise<int>& prom){
	int x;
	std::cout<<"Please, enter an integer value: ";
	std::cin.exceptions(std::ios::failbit);
	try{
		std::cin>>x;
		prom.set_value(x);
	}catch(std::exception& e){
		prom.set_exception(std::current_exception());
	}
}

void print_int2(std::future<int>& fut){
	try{
		 int x = fut.get(); // 获取共享状态的值.
		 std::cout << "value: " << x << '\n'; // 打印 value: 10.
	}catch(std::exception & e){
		 std::cout << "[exception caught: " << e.what() << "]\n";
	}
}
void test_promise_excp(){
	std::promise<int> prom;
	std::future<int> fut= prom.get_future();

	std::thread t1(get_int, std::ref(prom));
	std::thread t2(print_int2, std::ref(fut));

	t1.join();
	t2.join();
}

int countdown(int from, int to){
	for(int i = from; i!=to; --i){
		std::cout<<i<<"\n";
		std::this_thread::sleep_for(std::chrono::seconds(1));
	}
	std::cout << "Finished!\n";
	return from - to;
}
void test_package_task(){
	std::packaged_task<int(int, int)> ptask(countdown); // 设置 packaged_task
	auto fut = ptask.get_future();// 获得与 packaged_task 共享状态相关联的 future 对象.
	std::thread t1(std::move(ptask), 10 , 0);//创建一个新线程完成计数任务.
	int value = fut.get();// 等待任务完成并获取结果.
	std::cout << "The countdown lasted for " << value << " seconds.\n";
	t1.join();
}
void test_package_task2(){
	std::packaged_task<int(int)> foo; // 默认构造函数.
	// 使用 lambda 表达式初始化一个 packaged_task 对象.
	std::packaged_task<int(int)> bar([](int x){return x*2;});
	foo = std::move(bar); // move-赋值操作，也是 C++11 中的新特性.
	// 获取与 packaged_task 共享状态相关联的 future 对象.
	std::future<int> ret = foo.get_future();

	// 产生线程，调用被包装的任务.
//	std::thread(std::move(foo), 10).detach();
	std::thread t1(std::move(foo), 10);
	t1.join();

	int value = ret.get(); // 等待任务完成并获取结果.
	std::cout << "The double of 10 is " << value << ".\n";
}

// 在新线程中启动一个 int(int) packaged_task.
std::future<int> launcher(std::packaged_task<int(int)>& tsk, int arg)
{
    if (tsk.valid()) {
        std::future<int> ret = tsk.get_future();
        std::thread (std::move(tsk),arg).detach();
        return ret;
    }
    else return std::future<int>();
}
void test_package_task3(){
	std::packaged_task<int(int)> tsk0;
	std::packaged_task<int(int)> tsk([](int x){return x*2;});
	std::future<int> fut = launcher(tsk,25);
	std::cout << "The double of 25 is " << fut.get() << ".\n";
}

int triple (int x) { return x*3; }
void test_package_task4(){
		std::packaged_task<int(int)> tsk(triple); // package task

		std::future<int> fut = tsk.get_future();
		std::thread (std::move(tsk), 100).detach();
		std::cout << "The triple of 100 is " << fut.get() << ".\n";

		// re-use same task object: error move tsk, tsk is empty
		std::packaged_task<int(int)> tsk1(triple);
		tsk = std::move(tsk1);
	   tsk.reset();
	   fut = tsk.get_future();
	   std::thread(std::move(tsk), 200).detach();
	   std::cout << "Thre triple of 200 is " << fut.get() << ".\n";
}

bool is_prime(int x){
	for(int i =2; i<x; ++i){
		if( x % i == 0){
			return false;
		}
	}
	return true;
}
void test_future_asyn(){
	// call function asynchronously:
	int v = 44444443;
	std::future<bool> fut = std::async(std::launch::async, is_prime, v );
	// do something while waiting for function to set future:
   std::cout << "checking, please wait";
   std::chrono::milliseconds span(100);
   while(fut.wait_for(span) == std::future_status::timeout){
	   std::cout<<".";
    }
   // retrieve return value
	bool x = fut.get();
	std::cout << "\n " << v << (x ? "is" : "is not") << " prime.\n";
}

int do_get_value() { return 10; }
void test_future_share(){
	   std::future<int> fut = std::async(std::launch::async, do_get_value);
	   std::shared_future<int> shared_fut = fut.share();

	     // 共享的 future 对象可以被多次访问.
	    std::cout << "value: " << shared_fut.get() << '\n';
	    std::cout << "its double: " << shared_fut.get()*2 << '\n';
}

void test_future_valid(){
		// 由默认构造函数创建的 std::future 对象,
	    // 初始化时该 std::future 对象处于为 invalid 状态.
	    std::future<int> foo, bar;
	    foo = std::async(std::launch::async, do_get_value); // move 赋值, foo 变为 valid.
	    bar = std::move(foo); // move 赋值, bar 变为 valid, 而 move 赋值以后 foo 变为 invalid.

	    if (foo.valid())
	        std::cout << "foo's value: " << foo.get() << '\n';
	    else
	        std::cout << "foo is not valid\n";

	    if (bar.valid())
	        std::cout << "bar's value: " << bar.get() << '\n';
	    else
	        std::cout << "bar is not valid\n";
}

void test_future_wait(){
		// call function asynchronously:
	    std::future < bool > fut = std::async(std::launch::async, is_prime, 194232491);

	    std::cout << "Checking...\n";
	    fut.wait();

	    std::cout << "\n194232491 ";
	    if (fut.get()) // guaranteed to be ready (and not block) after wait returns
	        std::cout << "is prime.\n";
	    else
	        std::cout << "is not prime.\n";
}

void do_print_ten(char c, int ms)
{
    for (int i = 0; i < 10; ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(ms));
        std::cout << c;
    }
}
void test_future_async(){
	std::cout << "with launch::async:\n";
	std::future < void >foo =std::async(std::launch::async, do_print_ten, '*', 100);
	std::future < void >bar =std::async(std::launch::async, do_print_ten, '@', 200);
	// async "get" (wait for foo and bar to be ready):
	foo.get();
	bar.get();
   std::cout << "\n\n";

   std::cout << "with launch::deferred:\n";
   foo = std::async(std::launch::deferred, do_print_ten, '*', 100);
   bar = std::async(std::launch::deferred, do_print_ten, '@', 200);
   // deferred "get" (perform the actual calls):
   std::this_thread::sleep_for(std::chrono::seconds(1));
   std::cout << "after 1s call :\n";
   foo.get();
   bar.get();
   std::cout << '\n';
}
////////////////////////////////////////////////////////////////////////////
// condition_variable
std::mutex c_mtx;
std::condition_variable cv;
bool ready = false; // 全局标志位.

void do_print_id(int id){
	std::unique_lock<std::mutex> ulk(c_mtx);
	while(!ready){// 如果标志位不为 true, 则等待...
		cv.wait(ulk);// 当前线程被阻塞, 当全局标志位变为 true 之后,
	}
	// 线程被唤醒, 继续往下执行打印线程编号id.
	std::cout << "thread " << id << '\n';
}
void go(){
	std::unique_lock<std::mutex> ulk(c_mtx);
	ready = true;
	cv.notify_all();
}
void test_condition_variable(){
	std::thread threads[10];
	// spawn 10 threads:
	for (int i = 0; i < 10; ++i){
	        threads[i] = std::thread(do_print_id, i);
	}
   std::cout << "10 threads ready to race...\n";
	go(); // go!
	for (auto & th:threads){
	        th.join();
	}
}

int cargo = 0;
bool shipment_available()
{
    return cargo != 0;
}
// 消费者线程.
void consume(int n)
{
	for(int i =0 ; i < n ; ++i){
		std::unique_lock<std::mutex> ulk(c_mtx);
		cv.wait(ulk, shipment_available);
		std::cout << "consume:"<<cargo << '\n';
		cargo = 0;
	}
}
void test_condition_variable2(){
	// 消费者线程.
	std::thread consumer_thread(consume, 10);
	for(int i=0; i <10; i++){
		while(shipment_available()){
			std::this_thread::yield();
		}
		std::unique_lock<std::mutex> ulk(c_mtx);
		cargo = i+ 1;
		std::cout <<"create : "<< cargo << '\n';
		cv.notify_one();
	}
	consumer_thread.join();
}

int value;
void do_read_value()
{
    std::cin >> value;
    cv.notify_one();
}
void test_condition_variable_wait(){
	std::cout << "Please, enter an integer (I'll be printing dots): \n";
	std::thread th(do_read_value);

	std::mutex mtx;
	std::unique_lock<std::mutex> ulk(mtx);
	while(cv.wait_for(ulk, std::chrono::seconds(1)) == std::cv_status::timeout){
		std::cout << '.';
		std::cout.flush();
	}
	std::cout << "You entered: " << value << '\n';
	th.join();
}

void consumer()
{
	std::unique_lock<std::mutex> ulk(c_mtx);
	cv.wait(ulk, shipment_available);
	std::cout << "consumer:"<<cargo << '\n';
	cargo = 0;
}
void producer(int id)
{
    std::unique_lock < std::mutex > lck(c_mtx);
    cargo = id;
    std::cout << "producer:"<<cargo << '\n';
    cv.notify_one();
}
void test_condition_variable_consumer(){
	std::thread th1[10], th2[10];
	for(int i =0; i < 10; ++i){
		th1[i] = std::thread(consumer);
		th2[i] = std::thread(producer, i+1);
	}
	for(int i =0; i < 10; ++i){
			th1[i].join();
			th2[i].join();
	}
}
////////////////////////////////////////////////////////////////////////////
// atomic
std::atomic<bool> readyA(false);// can be checked without being set
std::atomic_flag winner = ATOMIC_FLAG_INIT;// always set when checked

void count1m(int id){
	while(!readyA){
		std::this_thread::yield();// 等待主线程中设置 ready 为 true.
	}
	for(int i = 0; i < 1000000; ++i){}
	if(!winner.test_and_set()){
		std::cout << "thread #" << id << " won!\n";
	}
}
void test_atomic(){
	std::vector<std::thread> threads;

	std::cout << "spawning 10 threads that count to 1 million...\n";
	for (int i = 1; i <= 10; ++i){
	        threads.push_back(std::thread(count1m, i));
	}
	readyA = true;
	for (auto & th:threads){
	        th.join();
	}
}

std::atomic_flag lock_stream = ATOMIC_FLAG_INIT;
std::stringstream stream;
void append_number(int x){
	 while(lock_stream.test_and_set()){
	 std::this_thread::yield();
//		std::this_thread::sleep_for(std::chrono::milliseconds(100));
	 }
	stream << "thread #" << x << '\n';
	lock_stream.clear();
}
void test_atomic_clear(){
		std::vector < std::thread > threads;
	    for (int i = 1; i <= 10; ++i){
	        threads.push_back(std::thread(append_number, i));
	     }
	    for (auto & th:threads){
	        th.join();
	    }
	   std::cout << stream.str() << std::endl;;
}

std::atomic_flag lock = ATOMIC_FLAG_INIT;
void f(int n){
	for(int n = 0; n < 50; ++n){
		while(lock.test_and_set(std::memory_order_acquire)){// acquire lock
			 std::this_thread::yield();
		}
		std::cout << "Output from thread " << n << '\n';
		lock.clear(std::memory_order_release);// release lock
	}
}
void test_atomic_lock(){
	std::vector<std::thread> threads;
	for (int i = 1; i <= 10; ++i) {
		threads.push_back(std::thread(f, i));
	}
	for (auto & th : threads) {
		th.join();
	}
}

std::atomic <int> foo(0);
void set_foo(int x)
{
    foo = x; // 调用 std::atomic::operator=().
}
void print_foo()
{
    while (foo == 0) { // wait while foo == 0
        std::this_thread::yield();
    }
    std::cout << "foo1: " << foo << '\n';
}
void test_atomic_set(){
	std::thread first(print_foo);
	std::thread second(set_foo, 10);
	first.join();
	second.join();
}

std::atomic <int> foo2(0);
void set_foo2(int x)
{
	foo.store(x, std::memory_order_relaxed); // 设置(store) 原子对象 foo 的值
}
void print_foo2(){
	int x;
	do{
		x = foo.load(std::memory_order_relaxed);// 读取(load) 原子对象 foo 的值
	}while(x == 0);
	std::cout << "foo2: " << x << '\n';
}
void test_atomic_set2(){
	std::thread first(print_foo2);
	std::thread second(set_foo2, 12);
	first.join();
	second.join();
}

std::atomic <bool> readyB(false);
std::atomic <bool>  winnerB(false);

void cout1m(int id){
	while(!readyB){
		std::this_thread::yield();
	}
	for(int i =0; i < 1000000; ++i){}
	if(!winnerB.exchange(true)){
		std::cout<<"thread#"<<id<<"win!"<<std::endl;
	}
}

void test_atomic_exchange(){
	std::vector<std::thread> vector;
	for(int i =0; i <10 ; ++i){
		vector.push_back(std::thread(cout1m, i));
	}
	readyB = true;
	for(auto& th : vector){
		th.join();
	}
}


struct Node {
	int value;
	Node* next;
};
std::atomic<Node*> list_head(nullptr);
void append(int value){
	Node* newNode = new Node{value, list_head};

	while(! list_head.compare_exchange_weak(newNode->next, newNode)){}
}
void test_atomic_exchange_weak(){
	std::vector<std::thread> threads;
	for (int i = 0; i < 10; ++i) threads.push_back(std::thread(append, i));
	for (auto& th : threads) th.join();

	for(Node* it = list_head; it != nullptr; it = it->next){
		std::cout<<" "<<it->value;
	}
	std::cout<<" \n";
}
void append2(int value){
	Node* newNode = new Node{value, list_head};

	while(! list_head.compare_exchange_strong(newNode->next, newNode)){}
}
void test_atomic_exchange_strong(){
	std::vector<std::thread> threads;
	for (int i = 0; i < 10; ++i) threads.push_back(std::thread(append, i));
	for (auto& th : threads) th.join();

	for(Node* it = list_head; it != nullptr; it = it->next){
		std::cout<<" "<<it->value;
	}
	std::cout<<" \n";
}
////////////////////////////////////////////////////////////////////////////
int main_test() {
//	test_atomic_exchange_strong();
//	test_atomic_exchange_weak();
//	test_atomic_exchange();
//	test_atomic_set();
//	test_atomic_set2();
//	test_atomic_lock();
//	test_atomic_clear();
//	test_atomic();
//	test_condition_variable_consumer();
//	test_condition_variable_wait();
//	test_condition_variable2();
//	test_condition_variable();
//	test_future_async();
//	test_future_wait();
//	test_future_valid();
//	test_future_share();
//	test_future_asyn();
//	test_package_task4();
//	test_package_task3();
//	test_package_task2();
//	test_package_task();
//	test_promise_excp();
//	test_promise_move();
//	test_future();
//	test_uniq_lock();
//	test_lock_guard();
//	test_timed_mutx();
//	test_thread_mutex();
//	test_thread3();
//	test_thread2();
//	test_thread();
//	test_rv(1);
//	test_rv();
//	test_pass_param();
	return 0;
}
