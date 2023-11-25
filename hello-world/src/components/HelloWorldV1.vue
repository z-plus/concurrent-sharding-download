<template>
<div style="margin:50px">
    <el-form label-width="80px" style="display:flex;justify-content: center">
        <el-form-item label="文件大小">
            <el-input v-model="totalSize" placeholder="请输入内容" style="width:200px" />
        </el-form-item>
        <el-form-item>
            <div class="demo-progress" style="display:flex;justify-content: center">
                <el-button :loading="loading" type="warning" v-if="errorChunks.error" style="margin-right:20px" @click="retry()">重试</el-button>
                <el-button :loading="loading" @click="downloadFile()" style="margin-right:20px" v-else>下载</el-button>
                <div>
                    <el-progress :percentage="process" style="margin: 0 auto;"></el-progress>
                </div>
            </div>
        </el-form-item>
    </el-form>
</div>
</template>

<script>
import axios from 'axios'

export default {
    data() {
        return {
            docx: null,
            showPassword: false,
            process: 0,
            fileName: "filename.zip",
            //错误的分片信息
            errorChunks: {
                chunk: {},
                chunksDownloaded: 0,
                error: false,
            },
            chunks: [],
            chunkData: [],
            totalSize: 205736714,
            loading:false
        }
    },
    mounted() {
        // this.test();
    },
    methods: {
        async retry() {
            this.errorChunks.error = false
            this.loading = true
            this.download(this.errorChunks.chunksDownloaded)
        },
        async downloadFile() {
            this.process = 0
            this.loading = true
            //const totalSize = 15909964;
            //const totalSize = 205736714;
            //const totalSize = 4681746627;
            const chunkSize = 1024 * 1024 * 50;
            const totalChunks = Math.ceil(this.totalSize / chunkSize);

            console.log(totalChunks);
            //起始分片
            let chunksDownloaded = 0;
            // 生成分块数组
            for (let i = 0; i < totalChunks; i++) {
                const start = i * chunkSize;
                let end = (i + 1) * chunkSize - 1;

                if (end >= this.totalSize) {
                    end = this.totalSize - 1;
                }

                this.chunks.push({
                    startByte: start,
                    endByte: end,
                    data: null
                });
            }

            try {
                this.download(chunksDownloaded)
            } catch (error) {
                console.log(error);
            }
          
        },
        async download(chunksDownloaded) {
            //备份
            let chunksDownloadedCopy = chunksDownloaded
            //获取当前的分片信息
            let chunk = this.chunks[chunksDownloaded]
            try {
                const response = await axios.get('/api/download/file', {
                    headers: {
                        'Range': `bytes=${chunk.startByte}-${chunk.endByte}`,
                    },
                    responseType: 'blob',
                    timeout: 5000
                });
                console.log(typeof response.data)
                this.chunkData[chunksDownloaded] = await response.data
                chunksDownloaded++;
                //进度条
                this.process = (parseFloat(chunksDownloaded / this.chunks.length) * 100).toFixed(2) * 1;
                //最后一片时，直接合成并下载文件
                if (chunksDownloaded == this.chunks.length) {
                    console.log('All chunks downloaded!');
                    // 合并所有分块数据
                    let mergedBlob = new Blob(this.chunkData);
                    // 下载完成后将内容合并并下载
                    const url = URL.createObjectURL(mergedBlob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = this.fileName;
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                    URL.revokeObjectURL(url);
                    // 更新下载进度
                    this.progressMessage = '文件下载完成';
                    this.progress = 100;
                    this.loading = false;
                    this.mergedBlobList = []
                    this.errorChunks = {
                        chunk: {},
                        chunksDownloaded: 0,
                        error: false
                    }
                    this.chunks = []
                    this.chunkData = []
                } else {
                    this.download(chunksDownloaded)
                }
            } catch (error) {
                this.errorChunks.chunk = chunk
                this.errorChunks.error = true
                this.errorChunks.chunksDownloaded = chunksDownloadedCopy
                this.$message({
                message: '下载失败，请重试',
                type: 'error'
                });
            }
            this.loading = false
        },

        saveFile(data) {
            const blob = new Blob([data]);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = this.fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        },
        mergeChunks(previousChunks, currentChunk) {
            if (!previousChunks) {
                return currentChunk;
            }
            const mergedChunks = new Uint8Array(previousChunks.length + currentChunk.length);
            mergedChunks.set(previousChunks, 0);
            mergedChunks.set(currentChunk, previousChunks.length);
            return mergedChunks;
        },
        // async downloadFile() {
        // //const totalSize = 15909964;
        //     //const totalSize = 205736714;
        //     const totalSize = 4681746627;
        //     const chunkSize = 1024 * 1024 * 50;
        //     const totalChunks = Math.ceil(totalSize / chunkSize);

        //     console.log(totalChunks);

        //     let chunksDownloaded = 0;
        //     const chunks = [];
        //     const errorChunks = [];

        //     // 生成分块数组
        //     for (let i = 0; i < totalChunks; i++) {
        //         const start = i * chunkSize;
        //         let end = (i + 1) * chunkSize - 1;

        //         if (end >= totalSize) {
        //         end = totalSize - 1;
        //         }

        //         chunks.push({
        //             startByte: start,
        //             endByte: end,
        //             data: null
        //         });
        //     }

        //     try {
        //         // 使用 Promise.all 控制并发下载数，每个 Promise 仅下载单个分块
        //         await Promise.all(chunks.map(async (chunk) => {
        //             try {
        //                 const response = await axios.get('http://localhost:8088/download/file', {
        //                     headers: {
        //                     'Range': `bytes=${chunk.startByte}-${chunk.endByte}`
        //                     },
        //                     responseType: 'arraybuffer'
        //                 });
        //                 chunk.data = new Uint8Array(response.data);
        //                 chunksDownloaded++;
        //             } catch (error) {
        //                 chunk.data = null;
        //                 //TODO 记录错误的片段
        //                 errorChunks.push(chunk)
        //             }
        //         }));

        //         if (errorChunks.length > 0) {
        //             console.log('error，点击重试');
        //             console.log(chunks);
        //             return;
        //         }
        //         console.log('All chunks downloaded!');

        //         // 合并所有分块数据
        //         let mergedFile = new Uint8Array(totalSize);
        //         let mergeOffset = 0;
        //         debugger;
        //         chunks.forEach((chunk) => {
        //             mergedFile.set(chunk.data, mergeOffset);
        //             mergeOffset += chunk.data.byteLength;
        //         });

        //         this.saveFile(mergedFile);
        //     } catch (error) {
        //         console.log(error);
        //     }
        // },
    }
}
</script>

<style scoped>
.demo-progress .el-progress--line {
    margin-bottom: 15px;
    width: 350px;
}
</style>
