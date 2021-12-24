
	/**
	 * 此方法使用协同过滤算法，大概意思就是从数据库拿到所有的用户收藏和当前的用户收藏作为比对，找出和当前用户收藏夹最接近的一个用户，
	 * 然后将那个用户的收藏中当前登录用户没有的展示给当前用户
	 * @param model
	 * @param request
	 * @return
	 */
	/*
	举例说明一下，比如当前登录userid = 1
	用户关联表中数据，
	用户1关联歌曲｛2，3｝（当前登录用户收藏的歌曲）
	用户2关联歌曲｛2，3，4｝
	用户3关联歌曲｛3，4，5｝
	用户4关联歌曲｛1，5｝
	*/
	@RequestMapping(value = "/getRecommendList", method = RequestMethod.GET)
	public String getRecommendList(Model model, HttpServletRequest request) {// 协同过滤算法
		HttpSession httpSession = request.getSession();
		String name = httpSession.getAttribute("name").toString();

		// 获取userId
		int orginal = userService.getUserByName(name).getId(); //对应模拟数据，这个值为1

		// 获取所有用户和歌曲的关联
		List<Enshrine> all = enshrineService.getAll();//刚才所有的模拟数据 第一个enshrine对象值为userid=1,movieid=2,第二个userid=1,movieid=2 。。。 这个集合的长度=所有用户收藏的歌曲的长度，所以说all集合的长度为10

		// 创建用户推荐map，数据结构为 key:Integer 对应用户id value:List 对应一个广告的id集合
		HashMap<Integer, List<Integer>> userRecommend = new HashMap();

		// 遍历所有的关联
		/*
		第1次循环 userid=1,movieid=2
		第2次循环 userid=1,movieid=3
		第3次循环 userid=2,movieid=2
		第4次循环 userid=2,movieid=3
		第5次循环 userid=2,movieid=4
		第6次循环 userid=3,movieid=3
		第7次循环 userid=3,movieid=4
		第8次循环 userid=3,movieid=5
		第9次循环 userid=4,movieid=1
		第10次循环 userid=4,movieid=5
		这个再看不懂的话，o(∩_∩)o 
		**/

		for (int i = 0; i < all.size(); i++) {
			// 获取每一个用户和歌曲Id
			int userId = all.get(i).getUser_id(); //第一次循环 userId=1
			int movieid = all.get(i).getMovie_id(); // 第一次循环 movie_id=2
			// 如果推荐map中有以当前用户为Key的数据
			if (userRecommend.containsKey(userId)) { //第一次循环，这个map是新建的，肯定都是Null，所以userRecommend肯定没有以当前用户id作为key的值
				// 根据用户id获取map对应的value
				List<Integer> recommendTemp = userRecommend.get(userId);
				// 在拿到的集合中添加新的歌曲id
				recommendTemp.add(movie_id);
				// 更新此key value
				userRecommend.put(userId, recommendTemp);
			} else {
				// 如果不包含，新建一个集合，然后将Key value放入map
				List<Integer> recommendTemp = new ArrayList<>();  //没有值就新建一个List
				recommendTemp.add(movie_id);					  // 添加当前循环的这个movieid,这个集合的数据就是 [2]
				userRecommend.put(userId, recommendTemp);         // 将当前循环的userid作为key，集合作为value放入userRecommend中，此时map中的值为｛1，[2]｝
			}
		}
		/*
		*每次循环userRecommend的值的变化
		{{1，[2]}}
		{{1，[2，3]}}
		{{1，[2，3]},{2，[2]}}
		{{1，[2，3]},{2，[2,3]}}
		{{1，[2，3]},{2，[2,3,4]}}
		{{1，[2，3]},{2，[2,3,4]},{3,[3]}}
		{{1，[2，3]},{2，[2,3,4]},{3,[3,4]}}
		{{1，[2，3]},{2，[2,3,4]},{3,[3,4,5]}}
		{{1，[2，3]},{2，[2,3,4]},{3,[3,4,5]}，{4,[1]}}
		{{1，[2，3]},{2，[2,3,4]},{3,[3,4,5]}，{4,[1，5]}} 最终形态
		*/

		//注意如果当前登录用户没有收藏过任何歌曲，这个map中就不会有以当前登录用户为key的value了


		// 新建我的歌单
		List<Integer> myRecommend = new ArrayList<>();
		// 如果刚才存放的map中包含有以当前登录用户id为key的数据
		if (userRecommend.containsKey(orginal)) { 
			// 则我的歌单 = 从歌单列表中获取的集合
			myRecommend = userRecommend.get(orginal);
			// System.out.println("orginal: " + orginal);
			// System.out.println("mySize: " + myRecommend.size());
		} else {
			myRecommend = new ArrayList<>();
		}

		/*
			从map中取出刚才存入的当前用户的歌单
			myRecommend = [2，3]
			如果用户没有收藏任何的歌曲
			myRecommend = []
			当前的模拟数据是有歌曲的，所以是第一个
		*/



		// 将我的歌单列表集合转换为set集合
		HashSet<Integer> myRecommendSet = new HashSet<Integer>(myRecommend);
		double maxValue = 0;
		int maxId = -1;
		// 遍历推荐列表中所有点key
		/*
		这个keySet()方法，相当于将userRecommend所有Key拿出来放入一个集合中
		数据结构就是[1,2,3,4]
		*/

		/*
			循环[1,2,3,4]

			第一次循环 key=1
			循环的是当前用户的id，所以跳过这次循环

			第二次 key=2
			从userRecommend拿到所有的歌曲集合
			拿到了[2,3,4]
			和我的歌曲集合myRecommend=[2，3]做对比
			取交集获取新集合 intersection = [2,3]
			创建一个新集合union放入我的歌曲集合union = [2,3]
			把当前循环用户的歌曲集合放入union，因为是set集合会去掉重复的元素所以 union=[2,3,4]
			用intersection此时的长度2/union此时的长度3 得到一个ratio小数 可以将它看作为67% ratio=0.67
			此时maxValue的值为0，0.67 > 0
			所以设置maxValue的值为0.67
			maxId = 当前循环的用户id = 2

			第三次 key=3
			从userRecommend拿到所有的歌曲集合
			拿到了[3,4,5]
			和我的歌曲集合myRecommend=[2，3]做对比
			取交集获取新集合 intersection = [3]
			创建一个新集合union放入我的歌曲集合union = [2,3]
			把当前循环用户的歌曲集合放入union，因为是set集合会去掉重复的元素所以 union=[2,3,4,5]
			用intersection此时的长度1/union此时的长度4 得到一个ratio小数  ratio=0.25
			此时maxValue的值为0.67，0.25 < 0.67
			所以maxValue的值不变 = 0.67
			maxId不变 = 2

			第四次 key=4
			从userRecommend拿到所有的歌曲集合
			拿到了[1，5]
			和我的歌曲集合myRecommend=[2，3]做对比
			取交集获取新集合 intersection = []
			创建一个新集合union放入我的歌曲集合union = [2,3]
			把当前循环用户的歌曲集合放入union，因为是set集合会去掉重复的元素所以 union=[2,3]
			用intersection此时的长度0/union此时的长度2 得到一个ratio小数 ratio=0
			此时maxValue的值为0.67，0 < 0.67
			所以maxValue的值不变 = 0.67
			maxId不变 = 2
		*/
		for (int key : userRecommend.keySet()) {
			// 当遍历到当前用户Id为key的时候，跳过此次循环，开始下一次循环
			if (key == orginal) {
				continue;
			}
			// 根据当前循环到的key获取value，也就是对应此用户歌曲的集合
			List<Integer> thisRecommend = userRecommend.get(key);
			// System.out.println("thisSize: " + thisRecommend.size());
			// 将当前歌曲的集合转换为set
			HashSet<Integer> thisRecommendSet = new HashSet<>(thisRecommend);
			// 将我的歌曲集合转换为set
			HashSet<Integer> intersection = new HashSet<>(myRecommendSet);
			// 取交集 我的歌曲会剩下两个集合中都有的歌曲id
			intersection.retainAll(thisRecommendSet);
			HashSet<Integer> union = new HashSet<>(myRecommendSet);
			// System.out.println("union1: " + union.size());
			// 将两个人收藏的所有歌曲去重并放入此集合中
			union.addAll(thisRecommendSet);
			// System.out.println("union2: " + union.size());
			// 如果取过交集的集合为空，说明当前循环到的用户和当前登录的用户没有收藏同一种歌曲，跳过此次循环，开始下一次循环
			if (union.size() == 0) {
				continue;
			} else {
				// 如果不为空，则用我的歌曲列表/交集集合
				double ratio = (double) intersection.size() / union.size();
				if (ratio > maxValue) {
					// 最大值就位两者之比
					maxValue = ratio;
					// maxId = 当前循环的用户
					maxId = key;
				}
			}
		}


		// 创建歌曲推荐列表
		List<Integer> MovieRecommendList = new ArrayList<>();
		//　如果ｍａｘＩＤ没有被更改过，则为当前登录用户ID
		if (maxId == -1) { //此时maxId = 2
			maxId = orginal;
		} else {
			// 如果被更改过，就从推荐列表中取出key为maxId（maxId为拥有最大交集的用户id） 的歌曲列表，
			HashSet<Integer> differenceTemp = new HashSet<>(userRecommend.get(maxId)); // differenceTemp = [2,3,4]
			// maxId用户歌单列表中的歌曲 - 我的歌单列表中的歌曲 = 我没有的歌曲
			differenceTemp.removeAll(myRecommendSet); // differenceTemp = [4] 所以，在推荐列表中就会出现id为4的歌曲，剩下的就是计算相似度和将歌曲传到前台了！
			MovieRecommendList = new ArrayList<Integer>(differenceTemp); 
		}
		// 一下代码就是从我没有的歌曲列表id中取得歌曲信息
		List<Advertisement> movies = new ArrayList<>();
		for (int i = 0; i < MovieRecommendList.size(); i++) {
			Advertisement movie = advertisementService.getMovieById(MovieRecommendList.get(i));
			movies.add(movie);
		}
		model.addAttribute("professor", movies);
//		System.out.println(movies.size());
		DecimalFormat df=new DecimalFormat("0.00");
		// 歌单相似度 = 我的歌单长度/(我的歌单长度 + 我没有的歌曲长度) * 100%
		String similar = "歌单相似度："+df.format((float)myRecommendSet.size()/(myRecommendSet.size()+movies.size())*100)+"%";
		System.out.println(similar);
		 model.addAttribute("similar", similar);
		return "recommend";
	}


