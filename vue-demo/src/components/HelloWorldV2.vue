<template>
<div>
    <div class="test-div">
        <el-form label-width="150px">
            <el-form-item label="文件地址">
                <el-input v-model="uploadPath" placeholder="请输入文件完整地址" style="width:200px" />   <el-button :loading="loading" @click="getFileInfo()" style="margin-right:20px">获取</el-button>
            </el-form-item>
            <el-form-item label="文件大小：">
                <el-input v-model="totalSize" disabled style="width:200px" />
            </el-form-item>
            <el-form-item label="分片的大小：">
                <el-input v-model="CHUNK_SIZE" disabled style="width:200px" />
            </el-form-item>
            <el-form-item label="最大并发请求数：">
                <el-input v-model="MAX_CONCURRENT_DOWNLOADS" disabled style="width:200px" />
            </el-form-item>
            <el-form-item label="最大合并数：">
                <el-input v-model="MAX_MERGE_DOWNLOADS" disabled  style="width:200px" />
            </el-form-item>
        </el-form>
    
        <div class="demo-progress" style="display:flex;">
            <el-button :loading="loading" type="warning" v-if="errorChunks.error" style="margin-right:20px" @click="retry()">重试</el-button>
            <el-button :loading="loading" @click="downloadFile()" style="margin-right:20px" v-else>下载</el-button>
            <div>
                <el-progress :percentage="process" style="margin: 0 auto;"></el-progress>
            </div>
        </div>
    </div>
</div>
</template>

<script>
import axios from 'axios'

export default {
    data() {
        return {
             loading:false,
            docx: null,
            showPassword: false,
            process: 0,
            fileName: "test.zip",
            //错误的分片信息
            errorChunks: {
                chunk: {},
                chunksDownloaded: 0,
                error: false,
            },
            chunks: [],
            chunkData: [],
            uploadPath: "",
            totalSize: 0,
            CHUNK_SIZE: 1024 * 1024 * 20, // 每个分组的大小
            MAX_CONCURRENT_DOWNLOADS: 1, // 最大并发请求数量
            MAX_MERGE_DOWNLOADS: 5, // 最大合并数  MAX_CONCURRENT_DOWNLOADS * MAX_MERGE_DOWNLOADS
            downloading: 0, // 正在下载的请求数量
            downloaded: 0, // 已经下载的文件长度
            mergedBlobList:[],
            list:[]
        }
    },
    methods: {
        async getFileInfo () {
            const response = await axios.post(`/api/download/getFileInfo`, {uploadPath:this.uploadPath});
            this.totalSize = response.data.size || 0;
            this.fileName = response.data.fileName;
        },
        async retry() {
            console.log(this.chunkData)
            console.log(this.errorChunks.chunk)
            this.loading = true;
            await this.download(this.errorChunks.chunk)
            this.errorChunks.error = false
            if(this.downloaded >= this.totalSize || this.chunkData.length == this.MAX_CONCURRENT_DOWNLOADS * this.MAX_MERGE_DOWNLOADS) {
                    this.mergedBlobList.push(new Blob(this.chunkData))
                    this.chunkData = []
            }
            this.mergerAndDownload()
        },
        async downloadFile() {
            this.process = 0;
            this.loading = true;
            const totalChunks = Math.ceil(this.totalSize / this.CHUNK_SIZE);
            console.log(totalChunks);
            // 生成分块数组
            for (let i = 0; i < totalChunks; i++) {
                const start = i * this.CHUNK_SIZE;
                let end = (i + 1) * this.CHUNK_SIZE - 1;

                if (end >= this.totalSize) {
                    end = this.totalSize - 1;
                }

                this.chunks.push({
                    index: i,
                    startByte: start,
                    endByte: end,
                    data: null
                });
            }

            this.mergerAndDownload();
        },
        async download(chunk) {
            try {
                const response = await axios.post('/api/download/file',{uploadPath:this.uploadPath}, {
                    headers: {
                        'Range': `bytes=${chunk.startByte}-${chunk.endByte}`
                    },
                    responseType: 'arraybuffer',
                    timeout: 51000
                });
                // if (chunk.index == 4 && this.errorChunks.error == false) {
                //      throw error;
                // }
                this.chunkData[chunk.index % (this.MAX_CONCURRENT_DOWNLOADS * this.MAX_MERGE_DOWNLOADS)] = new Uint8Array(await response.data);
                this.downloaded += chunk.endByte - chunk.startByte + 1; // 更新已下载的文件长度
                this.process = (parseFloat(this.downloaded / this.totalSize) * 100).toFixed(2) * 1;
            } catch (error) {
                console.error(`Failed to download chunk ${chunk.index}:`, error);
                this.errorChunks.chunk = chunk
                this.errorChunks.error = true
                this.loading = false;
                throw error;
            }
        },

        async mergerAndDownload() {
            const totalChunks = Math.ceil(this.totalSize / this.CHUNK_SIZE);
            let chunkQueue = [];
            while (this.downloaded < this.totalSize) {
                while (chunkQueue.length < this.MAX_CONCURRENT_DOWNLOADS && this.downloading < totalChunks) {
                    const chunk = this.chunks[this.downloading];
                    console.log(`Downloading chunk ${chunk.index}`);
                    chunkQueue.push(this.download(chunk));
                    this.downloading++;
                }
                await Promise.all(chunkQueue); // 等待数据块下载完成
                chunkQueue = []
                if(this.downloaded >= this.totalSize || this.chunkData.length == this.MAX_CONCURRENT_DOWNLOADS * this.MAX_MERGE_DOWNLOADS) {
                    this.mergedBlobList.push(new Blob(this.chunkData))
                    this.chunkData = []
                }
            }

            console.log('All chunks downloaded!');
            // 合并所有分块数据
            let mergedBlob = new Blob(this.mergedBlobList);
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
            this.process = 100;
            this.loading = false;
            // this.downloading = 0
            // this.downloaded = 0
            // this.mergedBlobList = []
            // this.errorChunks = {
            //     chunk: {},
            //     chunksDownloaded: 0,
            //     error: false
            // }
            // this.chunks = []
            // this.chunkData = []
        }
    }
}
</script>

<style scoped>
.demo-progress .el-progress--line {
    margin-bottom: 15px;
    width: 350px;
}

/* 容器div 样式 */
.content-div {
  position:relative;
}

/* 待居中div 样式*/
/* 水平 + 垂直 */
.test-div {
  position:absolute;
  top: 30%;
  left: 50%;
  transform:translate(-50%, -50%)
}

/* 水平 */
.test-div-sp {
  position:relative;
  left: 50%;
  transform:translate(-50%)
}

/* 垂直 */
.test-div-cz {
  position:absolute;
  top: 50%;
  transform:translate(0, -50%)
}

</style>
