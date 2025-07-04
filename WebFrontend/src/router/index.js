import { createRouter, createWebHistory } from 'vue-router'

const Login = () => import('../views/login.vue')
const Register = () => import('../views/register.vue')
const Manage = () => import('../views/manage.vue')
const DataOverview = () => import('../components/DataOverview.vue')
const DataCharts = () => import('../components/DataCharts.vue')
const UserManageDataChart = () => import('../components/UserManageDataChart.vue')
const CourseManageDataChart = () => import('../components/CourseManageDataChart.vue')
const ClassManageDataChart = () => import('../components/ClassManageDataChart.vue')

// 管理页面子组件
const HomeCards = () => import('../components/HomeCards.vue')
const UserManage = () => import('../components/UserManage.vue')
const ClassManage = () => import('../components/ClassManage.vue')
const CourseManage = () => import('../components/CourseManage.vue')
const FileManage = () => import('../components/FileManage.vue')

const routes=[
    {
        path:'/',
        redirect:'/login'
    },
    {
        path:'/login',
        component:Login
    },
    {
        path:'/register',
        component:Register
    },
    {
        path:'/manage',
        component:Manage,
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
                meta: { title: '系统管理 - 用户管理' }
            },
            {
                path: 'classes',
                component: ClassManage,
                meta: { title: '系统管理 - 班级管理' }
            },
            {
                path: 'courses',
                component: CourseManage,
                meta: { title: '系统管理 - 课程管理' }
            },
            {
                path: 'files',
                component: FileManage,
                meta: { title: '系统管理 - 文件管理' }
            },
            {
                path: 'data',
                component: DataOverview,
                meta: { title: '系统管理 - 数据概览' }
            }
        ]
    },
    {
        path:'/data-overview',
        component:DataOverview,
        meta: {
            requiresAuth: true,
            title: '数据概览'
        }
    },
    {
        path:'/course-data/:courseId',
        component:DataCharts,
        meta: {
            requiresAuth: true,
            title: '课程数据分析'
        }
    },
    {
        path:'/class-data/:classId',
        component:ClassManageDataChart,
        meta: {
            requiresAuth: true,
            title: '班级数据分析'
        }
    },
    {
        path:'/user-manage-data/:userId',
        component:UserManageDataChart,
        meta: {
            requiresAuth: true,
            title: '用户管理数据分析'
        }
    },
    {
        path:'/course-manage-data/:courseId',
        component:CourseManageDataChart,
        meta: {
            requiresAuth: true,
            title: '课程管理数据分析'
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
  })
  
router.beforeEach((to, from, next) => {
document.title = to.meta.title || '默认标题'

if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    
    if (token) {
    next() 
    } else {
    next({
        path: '/login',
        query: { redirect: to.fullPath } 
    })
    }
} else {

    if (to.path === '/login' && localStorage.getItem('token')) {
    next({ path: '/manage' })
    } else {
    next()
    }
}
})

export default router