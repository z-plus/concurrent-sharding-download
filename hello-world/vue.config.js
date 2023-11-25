const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    host: "0.0.0.0",// 允许外部ip访问
    port: 8080, // 自定义修改8080端口
    https: false,// 启用https
    open: false,//build自动打开浏览器
    proxy: {
      '/api': {
        target: 'http://10.20.20.211:8088',
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/' 
        }
      }
    }
  }
});
