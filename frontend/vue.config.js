const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 7021,
    proxy: {
      '/api': {
        target: 'http://localhost:7020',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:7020',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
