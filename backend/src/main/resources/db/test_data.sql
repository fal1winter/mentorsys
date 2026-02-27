-- 添加更多测试用户、导师和学生数据
-- 密码都是 123456，BCrypt 加密后的值

-- 新增导师用户 (user_type = 'mentor')
-- 密码都是 123456
INSERT INTO users (username, password, email, user_type, status) VALUES
('mentor_zhao', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'zhao@pku.edu.cn', 'mentor', 1),
('mentor_sun', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'sun@fudan.edu.cn', 'mentor', 1),
('mentor_wu', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'wu@nju.edu.cn', 'mentor', 1),
('mentor_zhou', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'zhou@ustc.edu.cn', 'mentor', 1),
('mentor_zheng', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'zheng@hit.edu.cn', 'mentor', 1),
('mentor_qian', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'qian@xjtu.edu.cn', 'mentor', 1);

-- 新增学生用户 (user_type = 'student')
-- 密码都是 123456
INSERT INTO users (username, password, email, user_type, status) VALUES
('student_wang', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'wang_stu@pku.edu.cn', 'student', 1),
('student_li', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'li_stu@tsinghua.edu.cn', 'student', 1),
('student_zhang', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'zhang_stu@zju.edu.cn', 'student', 1),
('student_liu', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'liu_stu@fudan.edu.cn', 'student', 1),
('student_chen', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'chen_stu@sjtu.edu.cn', 'student', 1),
('student_yang', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'yang_stu@nju.edu.cn', 'student', 1),
('student_huang', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'huang_stu@ustc.edu.cn', 'student', 1),
('student_xu', '$2a$10$YIqLDgRMnWRISg31KwaFo.clOw8bM4HeIEXm74tKKCaaFYyUOyGMa', 'xu_stu@hit.edu.cn', 'student', 1);

-- 新增导师数据
INSERT INTO mentors (name, title, institution, department, email, research_areas, keywords, bio, group_direction, expected_student_qualities, mentoring_style, available_positions, funding_status, collaboration_opportunities, accepting_students, max_students, current_students, rating_avg, rating_count, view_count, status, is_verified) VALUES
('赵伟教授', '教授', '北京大学', '信息科学技术学院', 'zhao@pku.edu.cn', 
 '["计算机视觉", "图像处理", "三维重建", "SLAM"]', 
 '["CV", "3D Vision", "SLAM", "NeRF", "Gaussian Splatting"]',
 '赵伟教授是北京大学信息科学技术学院教授，主要研究计算机视觉和三维重建。在CVPR、ICCV、ECCV等顶级会议发表论文40余篇。',
 '组内主要研究方向：1）神经辐射场与三维重建；2）视觉SLAM；3）多视图几何；4）自动驾驶感知',
 '期望学生具有扎实的数学基础（线性代数、优化理论），熟悉C++和Python，有计算机视觉相关项目经验者优先。',
 '每周一对一讨论，鼓励学生参与实际项目，提供充足的GPU计算资源。',
 4, '国家自然科学基金面上项目、华为合作项目', '与华为、商汤科技有深度合作',
 1, 6, 2, 4.75, 28, 15, 1, 1),

('孙明研究员', '研究员', '复旦大学', '计算机科学技术学院', 'sun@fudan.edu.cn',
 '["自然语言处理", "对话系统", "情感分析", "文本生成"]',
 '["NLP", "Dialogue", "Sentiment", "Text Generation", "ChatBot"]',
 '孙明研究员是复旦大学计算机学院研究员，专注于自然语言处理和对话系统研究。在ACL、EMNLP等会议发表论文35篇。',
 '组内研究方向：1）大模型对话系统；2）情感计算；3）多轮对话理解；4）知识增强的文本生成',
 '期望学生对NLP有浓厚兴趣，具备良好的编程能力，有论文发表经验者优先。',
 '采用小组讨论与个人指导相结合的方式，每周组会，鼓励学生参加学术会议。',
 3, '国家重点研发计划、腾讯合作项目', '与腾讯AI Lab、百度NLP有合作',
 1, 5, 1, 4.65, 22, 12, 1, 1),

('吴刚教授', '教授', '南京大学', '人工智能学院', 'wu@nju.edu.cn',
 '["强化学习", "多智能体系统", "博弈论", "机器人控制"]',
 '["RL", "Multi-Agent", "Game Theory", "Robotics", "Decision Making"]',
 '吴刚教授是南京大学人工智能学院教授，主要研究强化学习和多智能体系统。在NeurIPS、ICML、ICLR发表论文45篇。',
 '组内研究方向：1）深度强化学习算法；2）多智能体协作与竞争；3）机器人运动控制；4）自动驾驶决策',
 '期望学生具有扎实的数学基础，熟悉强化学习基本算法，有编程实现能力。',
 '注重理论与实践结合，提供机器人实验平台，支持学生参加机器人竞赛。',
 3, '国家自然科学基金重点项目、大疆合作项目', '与大疆、蔚来汽车有合作',
 1, 5, 2, 4.80, 30, 18, 1, 1),

('周芳副教授', '副教授', '中国科学技术大学', '计算机科学与技术学院', 'zhou@ustc.edu.cn',
 '["数据挖掘", "社交网络分析", "推荐系统", "用户行为分析"]',
 '["Data Mining", "Social Network", "RecSys", "User Behavior", "Graph Mining"]',
 '周芳副教授是中科大计算机学院副教授，主要研究数据挖掘和社交网络分析。在KDD、WWW、WSDM发表论文30篇。',
 '组内研究方向：1）图神经网络与社交网络；2）个性化推荐算法；3）用户行为建模；4）在线学习',
 '期望学生具有良好的数学基础和编程能力，对数据分析有兴趣，有竞赛经验者优先。',
 '采用项目驱动的培养模式，学生可参与实际数据分析项目，提供丰富的数据资源。',
 2, '国家自然科学基金青年项目、字节跳动合作项目', '与字节跳动、快手有合作',
 1, 4, 1, 4.55, 18, 10, 1, 1),

('郑涛教授', '教授', '哈尔滨工业大学', '计算学部', 'zheng@hit.edu.cn',
 '["知识图谱", "信息抽取", "问答系统", "语义理解"]',
 '["Knowledge Graph", "IE", "QA", "Semantic", "Entity Linking"]',
 '郑涛教授是哈工大计算学部教授，主要研究知识图谱和信息抽取。在ACL、EMNLP、NAACL发表论文50余篇。',
 '组内研究方向：1）大规模知识图谱构建；2）开放域信息抽取；3）知识增强的问答系统；4）事件抽取',
 '期望学生具有NLP基础，熟悉深度学习框架，有知识图谱相关经验者优先。',
 '每周组会讨论，提供充足的计算资源，鼓励学生参与开源项目。',
 4, '国家重点研发计划、美团合作项目', '与美团、京东有深度合作',
 1, 6, 3, 4.70, 35, 20, 1, 1),

('钱学森教授', '教授', '西安交通大学', '电子与信息学部', 'qian@xjtu.edu.cn',
 '["医学图像分析", "计算机辅助诊断", "深度学习", "医疗AI"]',
 '["Medical Imaging", "CAD", "Healthcare AI", "Segmentation", "Detection"]',
 '钱学森教授是西安交大电信学部教授，主要研究医学图像分析和计算机辅助诊断。在MICCAI、TMI、MedIA发表论文40篇。',
 '组内研究方向：1）医学图像分割与检测；2）病理图像分析；3）多模态医学影像融合；4）可解释医疗AI',
 '期望学生具有深度学习基础，对医学AI有兴趣，有图像处理经验者优先。',
 '与多家医院合作，提供真实医学数据，注重培养学生的跨学科能力。',
 3, '国家自然科学基金重点项目、医院合作项目', '与西京医院、华西医院有合作',
 1, 5, 2, 4.60, 25, 14, 1, 1);


-- 新增学生数据
INSERT INTO students (user_id, name, email, current_institution, major, degree_level, graduation_year, gpa, research_interests, keywords, bio, personal_abilities, expected_research_direction, preferred_mentor_style, programming_skills, publications_count, project_experience, status) VALUES
((SELECT id FROM users WHERE username = 'student_wang'), '王小明', 'wang_stu@pku.edu.cn', '北京大学', '计算机科学与技术', '硕士', 2027, 3.85,
 '["计算机视觉", "深度学习", "图像识别"]',
 '["CV", "Deep Learning", "Image Recognition", "CNN"]',
 '北京大学计算机系硕士研究生，对计算机视觉和深度学习有浓厚兴趣。',
 '具有扎实的数学基础，熟悉PyTorch和TensorFlow，有图像分类项目经验。',
 '希望从事计算机视觉方向的研究，特别是三维重建和神经渲染。',
 '希望导师能够提供充足的指导和计算资源，有定期的一对一讨论。',
 '["Python", "PyTorch", "C++", "OpenCV"]',
 1, '参与过图像分类竞赛，获得省级二等奖；完成过基于深度学习的目标检测项目。', 1),

((SELECT id FROM users WHERE username = 'student_li'), '李晓华', 'li_stu@tsinghua.edu.cn', '清华大学', '人工智能', '博士', 2028, 3.92,
 '["自然语言处理", "大语言模型", "对话系统"]',
 '["NLP", "LLM", "Dialogue", "Transformer", "BERT"]',
 '清华大学人工智能专业博士研究生，专注于自然语言处理研究。',
 '具有优秀的编程能力和科研能力，已发表2篇顶会论文。',
 '希望深入研究大语言模型的理论和应用，探索更高效的训练方法。',
 '希望有充足的学术自由度，能够参加国际学术会议。',
 '["Python", "PyTorch", "Transformers", "Linux"]',
 2, '参与过多个NLP项目，包括情感分析、文本分类、对话系统等。', 1),

((SELECT id FROM users WHERE username = 'student_zhang'), '张伟', 'zhang_stu@zju.edu.cn', '浙江大学', '软件工程', '硕士', 2026, 3.78,
 '["推荐系统", "机器学习", "数据挖掘"]',
 '["RecSys", "ML", "Data Mining", "CTR", "Embedding"]',
 '浙江大学软件工程专业硕士研究生，对推荐系统和数据挖掘感兴趣。',
 '熟悉常见的机器学习算法，有实际的推荐系统开发经验。',
 '希望从事推荐系统方向的研究，探索更精准的个性化推荐算法。',
 '希望能够参与实际的工业项目，积累实践经验。',
 '["Python", "Spark", "SQL", "TensorFlow"]',
 0, '在阿里巴巴实习期间参与推荐系统优化项目。', 1),

((SELECT id FROM users WHERE username = 'student_liu'), '刘芳', 'liu_stu@fudan.edu.cn', '复旦大学', '计算机应用技术', '硕士', 2027, 3.88,
 '["强化学习", "机器人", "自动驾驶"]',
 '["RL", "Robotics", "Autonomous Driving", "Control", "Planning"]',
 '复旦大学计算机应用技术专业硕士研究生，对强化学习和机器人控制感兴趣。',
 '具有良好的数学基础，熟悉强化学习算法，有机器人项目经验。',
 '希望从事强化学习在机器人控制中的应用研究。',
 '希望有机会参与实际的机器人项目，接触真实的硬件平台。',
 '["Python", "ROS", "C++", "MATLAB"]',
 1, '参与过RoboMaster机器人竞赛，负责决策算法开发。', 1),

((SELECT id FROM users WHERE username = 'student_chen'), '陈杰', 'chen_stu@sjtu.edu.cn', '上海交通大学', '电子信息', '博士', 2029, 3.95,
 '["医学图像", "深度学习", "计算机辅助诊断"]',
 '["Medical Imaging", "Deep Learning", "CAD", "Segmentation"]',
 '上海交通大学电子信息专业博士研究生，专注于医学图像分析研究。',
 '具有扎实的图像处理基础，熟悉医学影像数据，已发表1篇顶会论文。',
 '希望深入研究医学图像分析，开发更准确的计算机辅助诊断系统。',
 '希望能够与医院合作，接触真实的临床数据。',
 '["Python", "PyTorch", "ITK", "SimpleITK"]',
 1, '参与过肺结节检测项目，在LUNA16数据集上取得优秀成绩。', 1),

((SELECT id FROM users WHERE username = 'student_yang'), '杨洋', 'yang_stu@nju.edu.cn', '南京大学', '人工智能', '硕士', 2026, 3.72,
 '["知识图谱", "信息抽取", "问答系统"]',
 '["Knowledge Graph", "IE", "QA", "NER", "Relation Extraction"]',
 '南京大学人工智能专业硕士研究生，对知识图谱和信息抽取感兴趣。',
 '熟悉NLP基础技术，有知识图谱构建经验。',
 '希望从事知识图谱方向的研究，探索更高效的信息抽取方法。',
 '希望有定期的组会讨论，能够与同学交流学习。',
 '["Python", "Neo4j", "SPARQL", "HuggingFace"]',
 0, '参与过企业知识图谱构建项目，负责实体识别模块。', 1),

((SELECT id FROM users WHERE username = 'student_huang'), '黄磊', 'huang_stu@ustc.edu.cn', '中国科学技术大学', '计算机科学', '硕士', 2027, 3.80,
 '["图神经网络", "社交网络分析", "图表示学习"]',
 '["GNN", "Social Network", "Graph Learning", "Node Classification"]',
 '中科大计算机科学专业硕士研究生，对图神经网络和社交网络分析感兴趣。',
 '具有良好的数学基础，熟悉图神经网络算法。',
 '希望从事图神经网络方向的研究，探索更强大的图表示学习方法。',
 '希望能够参与实际的数据分析项目。',
 '["Python", "PyTorch Geometric", "DGL", "NetworkX"]',
 0, '参与过社交网络用户行为分析项目。', 1),

((SELECT id FROM users WHERE username = 'student_xu'), '徐静', 'xu_stu@hit.edu.cn', '哈尔滨工业大学', '计算机应用技术', '博士', 2028, 3.90,
 '["多模态学习", "视觉语言", "跨模态检索"]',
 '["Multimodal", "Vision-Language", "Cross-modal", "CLIP", "VQA"]',
 '哈工大计算机应用技术专业博士研究生，专注于多模态学习研究。',
 '具有扎实的深度学习基础，熟悉视觉和语言模型，已发表2篇论文。',
 '希望深入研究多模态学习，探索视觉和语言的深度融合。',
 '希望有充足的计算资源和学术交流机会。',
 '["Python", "PyTorch", "Transformers", "CUDA"]',
 2, '参与过视觉问答和图文匹配项目，在多个数据集上取得SOTA结果。', 1);
