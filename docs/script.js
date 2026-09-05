const levels=['none','low','medium','high','full'];
const colors={none:'#E2E6E1',low:'#B7D9C5',medium:'#72B58A',high:'#3E8B61',full:'#145A3A'};
const night={none:'#21262D',low:'#0E4429',medium:'#006D32',high:'#26A641',full:'#39D353'};
function fillGrid(id,count,nightMode=false){const el=document.getElementById(id);if(!el)return;const frag=document.createDocumentFragment();for(let i=0;i<count;i++){const s=document.createElement('span');let level;if(i>count*.58)level='none';else if(i%17===0)level='low';else if(i%7===0)level='medium';else if(i%4===0)level='high';else level='full';s.style.background=(nightMode?night:colors)[level];if(i===Math.floor(count*.31)){s.style.outline=nightMode?'1px solid #58A6FF':'1px solid #2D6A4F';s.style.outlineOffset='3px'}frag.appendChild(s)}el.appendChild(frag)}
fillGrid('heroGrid',98);fillGrid('paletteGrid',72);fillGrid('phoneGrid',70,true);
