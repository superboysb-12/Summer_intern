import { createRouter, createWebHistory } from 'vue-router'



const Login = () => import('../views/login.vue')
const Register = () => import('../views/register.vue')

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
    next({ path: '/home' })
    } else {
    next()
    }
}
})

export default router