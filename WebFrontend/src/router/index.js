import { createRouter, createWebHistory } from 'vue-router'

const Login = () => import('../views/login.vue')
const Register = () => import('../views/register.vue')
const Manage = () => import('../views/manage.vue')
const DataCharts = () => import('../components/DataCharts.vue')
const UserManageDataChart = () => import('../components/UserManageDataChart.vue')
const CourseManageDataChart = () => import('../components/CourseManageDataChart.vue')
const ClassManageDataChart = () => import('../components/ClassManageDataChart.vue')
const EnrollmentStats = () => import('../components/admin/EnrollmentStats.vue')
const UsageStatistics = () => import('../components/UsageStatistics.vue')

// 管理页面子组件
const HomeCards = () => import('../components/HomeCards.vue')
const UserManage = () => import('../components/UserManage.vue')
const ClassManage = () => import('../components/ClassManage.vue')
const CourseManage = () => import('../components/CourseManage.vue')
const FileManage = () => import('../components/FileManage.vue')
const RAGManage = () => import('../components/RAGManage.vue')
const DeepSeekChat = () => import('../components/DeepSeekChat.vue')
const TeachingPlanGenerator = () => import('../components/TeachingPlanGenerator.vue')
const TeachingPlanEditor = () => import('../components/TeachingPlanEditor.vue')
const QuestionGenerator = () => import('../components/QuestionGenerator.vue')
const UtilityTools = () => import('../components/UtilityTools.vue')

// 学生专属组件
const StudentCourses = () => import('../components/student/StudentCourses.vue')
const StudentAssignments = () => import('../components/student/StudentAssignments.vue')
const StudentAssignmentDetail = () => import('../components/student/StudentAssignmentDetail.vue')
const StudentResources = () => import('../components/student/StudentResources.vue')
const StudentRecords = () => import('../components/student/StudentRecords.vue')

// 角色权限常量
const ROLES = {
    ADMIN: 'ADMIN',
    TEACHER: 'TEACHER',
    STUDENT: 'STUDENT'
}

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        component: Login
    },
    {
        path: '/register',
        component: Register
    },
    {
        path: '/manage',
        component: Manage,
        meta: {
            requiresAuth: true,
            title: '系统管理'
        },
        children: [
            {
                path: '',
                component: HomeCards,
                meta: { title: '系统管理 - 首页' }
            },
            {
                path: 'users',
                component: UserManage,
                meta: {
                    title: '系统管理 - 用户管理',
                    roles: [ROLES.ADMIN]  // 仅管理员可访问
                }
            },
            {
                path: 'classes',
                component: ClassManage,
                meta: {
                    title: '系统管理 - 班级管理',
                    roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
                }
            },
            // 教师和管理员的课程管理
            {
                path: 'courses',
                component: CourseManage,
                meta: {
                    title: '系统管理 - 课程管理',
                    roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
                }
            },
            // 学生的课程页面
            {
                path: 'student-courses',
                component: StudentCourses,
                meta: {
                    title: '系统管理 - 我的课程',
                    roles: [ROLES.STUDENT]  // 仅学生可访问
                }
            },
            {
                path: 'files',
                component: FileManage,
                meta: {
                    title: '系统管理 - 文件管理',
                    roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT]  // 所有角色可访问
                }
            },

            {
                path: 'enrollment-stats',
                component: EnrollmentStats,
                meta: {
                    title: '系统管理 - 选课统计',
                    roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
                }
            },
            {
                path: 'rag',
                component: RAGManage,
                meta: {
                    title: '系统管理 - RAG知识库',
                    roles: [ROLES.ADMIN]  // 仅管理员可访问
                }
            },
            {
                path: 'teaching-plan',
                component: TeachingPlanGenerator,
                meta: {
                    title: '系统管理 - 教案生成',
                    roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
                }
            },
            {
                path: 'question-generator',
                component: QuestionGenerator,
                meta: {
                    title: '系统管理 - 题目生成',
                    roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT]  // 管理员和教师可访问
                }
            },
            {
                path: 'deepseek',
                component: DeepSeekChat,
                meta: {
                    title: '系统管理 - DeepSeek Chat',
                    roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT]  // 所有角色可访问
                }
            },
            {
                path: 'tools',
                component: UtilityTools,
                meta: {
                    title: '系统管理 - 实用工具',
                    roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT]  // 所有角色可访问
                }
            },
            // 学生专属页面
            {
                path: 'assignments',
                component: StudentAssignments,
                meta: {
                    title: '系统管理 - 作业管理',
                    roles: [ROLES.STUDENT]  // 仅学生可访问
                }
            },
            {
                path: 'assignment/:id',
                component: StudentAssignmentDetail,
                meta: {
                    title: '系统管理 - 作业题目',
                    roles: [ROLES.STUDENT]  // 仅学生可访问
                }
            },
            {
                path: 'study-records',
                component: StudentRecords,
                meta: {
                    title: '系统管理 - 学习记录',
                    roles: [ROLES.STUDENT]  // 仅学生可访问
                }
            },
            {
                path: 'resources',
                component: StudentResources,
                meta: {
                    title: '系统管理 - 学习资源',
                    roles: [ROLES.STUDENT]  // 仅学生可访问
                }
            },
            // 添加通知管理路由
            {
                path: 'messages',
                component: () => import('../components/MessageManage.vue'),
                meta: {
                    title: '系统管理 - 通知管理',
                    roles: [ROLES.ADMIN]  // 仅管理员可访问
                }
            },
            // 添加私信路由
            {
                path: 'private-messages',
                component: () => import('../components/PrivateMessaging.vue'),
                meta: {
                    title: '系统管理 - 私信',
                    roles: [ROLES.ADMIN, ROLES.TEACHER, ROLES.STUDENT]  // 所有角色可访问
                }
            },
            // 使用统计
            {
                path: 'usage-statistics',
                component: UsageStatistics,
                meta: {
                    title: '系统管理 - 使用统计',
                    roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
                }
            }
        ]
    },

    {
        path: '/course-data/:courseId',
        component: DataCharts,
        meta: {
            requiresAuth: true,
            title: '课程数据分析',
            roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
        }
    },
    {
        path: '/class-data/:classId',
        component: ClassManageDataChart,
        meta: {
            requiresAuth: true,
            title: '班级数据分析',
            roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
        }
    },
    {
        path: '/user-manage-data/:userId',
        component: UserManageDataChart,
        meta: {
            requiresAuth: true,
            title: '用户管理数据分析',
            roles: [ROLES.ADMIN]  // 仅管理员可访问
        }
    },
    {
        path: '/course-manage-data/:courseId',
        component: CourseManageDataChart,
        meta: {
            requiresAuth: true,
            title: '课程管理数据分析',
            roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
        }
    },
    {
        path: '/teaching-plan-editor/:id',
        component: TeachingPlanEditor,
        meta: {
            requiresAuth: true,
            title: '教案在线编辑',
            roles: [ROLES.ADMIN, ROLES.TEACHER]  // 管理员和教师可访问
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    document.title = to.meta.title || '默认标题'

    // 检查是否需要认证
    if (to.meta.requiresAuth) {
        const token = localStorage.getItem('token')
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const userRole = userInfo.userRole || 'STUDENT'

        if (!token) {
            next({
                path: '/login',
                query: { redirect: to.fullPath }
            })
            return
        }

        // 检查角色权限
        if (to.meta.roles && !to.meta.roles.includes(userRole)) {
            // 用户没有权限访问该页面
            next({ path: '/manage' }) // 重定向到有权限的主页面
            return
        }

        next()
    } else {
        if (to.path === '/login' && localStorage.getItem('token')) {
            next({ path: '/manage' })
        } else {
            next()
        }
    }
})

export default router