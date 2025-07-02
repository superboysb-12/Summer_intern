import { createRouter, createWebHistory } from 'vue-router'



const Login = () => import('../views/login.vue')
const Register = () => import('../views/register.vue')
const Manage = () => import('../views/manage.vue')
const DataOverview = () => import('../components/DataOverview.vue')
const DataCharts = () => import('../components/DataCharts.vue')

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
        }
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
        component:DataCharts,
        meta: {
            requiresAuth: true,
            title: '班级数据分析'
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